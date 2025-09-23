package com.techyplanet.scriptdeployer.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HexFormat;


public class CommonUtils {

	public static Comparator<File> scriptPrioritySorter(String fileRegexPattern, boolean sameSequenceAllowed) {
		return scriptPrioritySorter(fileRegexPattern, true, sameSequenceAllowed);
	}

	public static Comparator<File> scriptPrioritySorter(String fileRegexPattern, boolean seqNumApplicable,
			boolean sameSequenceAllowed) {

		return new Comparator<File>() {

			@Override
			public int compare(File file1, File file2) {

				String file1Name = file1.getName();
				String file2Name = file2.getName();

				int compareVal = getFileSequence(fileRegexPattern, file1Name, seqNumApplicable)
						.compareTo(getFileSequence(fileRegexPattern, file2Name, seqNumApplicable));
				if (compareVal == 0 && !sameSequenceAllowed) {
					throw new RuntimeException(
							String.format("No two scripts can have same sequence number allocated --> [%s] && [%s]",
									file1.getAbsolutePath(), file2.getAbsolutePath()));
				}
				if (compareVal == 0)
					compareVal = file1Name.compareTo(file2Name);
				if (compareVal == 0)
					throw new RuntimeException(String.format(
							"No two scripts should have same name even if exist in different folders --> [%s] && [%s]",
							file1.getAbsolutePath(), file2.getAbsolutePath()));
				return compareVal;
			}
		};
	}

	public static Long getFileSequence(String oneTimeFileRegexPattern, String fileName) {
		return getFileSequence(oneTimeFileRegexPattern, fileName, true);
	}

	public static Long getFileSequence(String filePattern, String fileName, boolean seqNumApplicable) {

		Long fileSequence = 0L;
		if (!seqNumApplicable)
			return fileSequence;

		Pattern p = Pattern.compile(filePattern);
		Matcher m = p.matcher(fileName);
		if (m.find()) {
			fileSequence = Long.parseLong(m.group(1));
		} else {
			fileSequence = 0L;
		}
		return fileSequence;
	}

	public static String generateFileChecksum(Path path) {
		MessageDigest messageDigest;
		byte[] digest;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(Files.readAllBytes(path));
			digest = messageDigest.digest();
		} catch (NoSuchAlgorithmException | IOException ex) {
			throw new RuntimeException("Checksum of file can't be generated.", ex);
		}
		String checksum = HexFormat.of().formatHex(digest).toUpperCase();
		if (checksum == null) {
			throw new RuntimeException("Checksum of file can't be generated.");
		}
		return checksum;
	}
}
