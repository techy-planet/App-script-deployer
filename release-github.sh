#!/usr/bin/env bash
set -euo pipefail

# GitHub Release helper for Script Deployer
#
# Purpose:
# - Determine the latest semantic version tag in the repo (e.g., 3.0.0) unless --version is provided
# - Build the distribution tarball using Gradle
# - Create a GitHub release with:
#     tag: <version> (e.g., 3.0.0)
#     title: v.<version> (e.g., v.3.0.0)
#     notes: contents of RELEASE_NOTES_<version>.md
#     asset: the generated bundle tar.gz from build/distributions
#
# Requirements:
# - GitHub CLI (gh) installed and authenticated (gh auth status)
# - git available
# - Java toolchain suitable for Gradle build
#
# Usage:
#   ./release-github.sh                # uses the latest semver tag in repo (e.g., 3.0.0)
#   ./release-github.sh --version 3.0.0
#   REPO_SLUG=owner/repo ./release-github.sh  # override repo if needed
#
# Notes:
# - This script uses the existing git tag as the release tag (e.g., 3.0.0) and sets the release title to v.<version>.
# - If a release for the tag already exists, pass --force to recreate.

FORCE=false
VERSION=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --version)
      VERSION="${2:-}"; shift 2;;
    --force)
      FORCE=true; shift;;
    -h|--help)
      sed -n '1,60p' "$0"; exit 0;;
    *)
      echo "Unknown argument: $1" >&2; exit 2;;
  esac
done

# Ensure gh is available and authenticated
if ! command -v gh >/dev/null 2>&1; then
  echo "ERROR: GitHub CLI (gh) is required. Install from https://cli.github.com/" >&2
  exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  echo "ERROR: gh is not authenticated. Run: gh auth login" >&2
  exit 1
fi

# Determine repo slug
if [[ -n "${REPO_SLUG:-}" ]]; then
  REPO="$REPO_SLUG"
else
  origin_url=$(git config --get remote.origin.url || true)
  if [[ -z "$origin_url" ]]; then
    echo "ERROR: Could not determine git remote origin URL. Set REPO_SLUG=owner/repo or configure git remote." >&2
    exit 1
  fi
  # Normalize to owner/repo
  if [[ "$origin_url" =~ ^git@([^:]+):([^/]+)/([^\.]+)(\.git)?$ ]]; then
    REPO="${BASH_REMATCH[2]}/${BASH_REMATCH[3]}"
  elif [[ "$origin_url" =~ ^https?://[^/]+/([^/]+)/([^\.]+)(\.git)?$ ]]; then
    REPO="${BASH_REMATCH[1]}/${BASH_REMATCH[2]}"
  else
    echo "ERROR: Unable to parse REPO from origin URL: $origin_url. Set REPO_SLUG=owner/repo." >&2
    exit 1
  fi
fi

echo "[info] Using repository: $REPO"

# Determine version (latest semver tag if not provided)
if [[ -z "$VERSION" ]]; then
  VERSION=$(git tag --sort=-v:refname | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | head -n1 || true)
  if [[ -z "$VERSION" ]]; then
    echo "ERROR: Could not find a semver tag like 3.0.0. Provide --version X.Y.Z." >&2
    exit 1
  fi
fi

echo "[info] Releasing version: $VERSION (title will be v.$VERSION)"

NOTES_FILE="RELEASE_NOTES_${VERSION}.md"
if [[ ! -f "$NOTES_FILE" ]]; then
  echo "ERROR: Release notes file not found: $NOTES_FILE" >&2
  exit 1
fi

# Build the project (skip tests to speed up if desired)
if [[ -x ./gradlew ]]; then
  echo "[info] Building distribution with Gradle..."
  ./gradlew --no-daemon clean build -x test
else
  echo "ERROR: gradlew not found. Ensure you're at the project root." >&2
  exit 1
fi

# Locate the newest bundle tarball
ARTIFACT=$(ls -t build/distributions/script-deployer-*-bundle.tar.gz 2>/dev/null | head -n1 || true)
if [[ -z "$ARTIFACT" ]]; then
  echo "ERROR: Could not find bundle tarball in build/distributions. Expected script-deployer-*-bundle.tar.gz" >&2
  exit 1
fi

echo "[info] Artifact: $ARTIFACT"

# Check if release already exists
set +e
EXISTS_JSON=$(gh release view "$VERSION" --repo "$REPO" --json tagName 2>/dev/null)
RC=$?
set -e
if [[ $RC -eq 0 && -n "$EXISTS_JSON" ]]; then
  if [[ "$FORCE" == true ]]; then
    echo "[warn] Release $VERSION exists. Deleting due to --force..."
    gh release delete "$VERSION" --repo "$REPO" --yes
  else
    echo "ERROR: Release for tag $VERSION already exists. Use --force to recreate." >&2
    exit 1
  fi
fi

# Create the release
echo "[info] Creating GitHub release $VERSION (title: v.$VERSION) and uploading asset..."

gh release create "$VERSION" "$ARTIFACT" \
  --repo "$REPO" \
  --title "v.$VERSION" \
  --notes-file "$NOTES_FILE" \
  --latest

echo "[success] Release created: $(gh release view "$VERSION" --repo "$REPO" --json url -q .url)"
