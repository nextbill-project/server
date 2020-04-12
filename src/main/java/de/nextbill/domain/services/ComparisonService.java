/*
 * NextBill server application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.domain.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ComparisonService {

	public double similarityWithComparableStrings(String string1, String string2) {
		String s1 = makeStringComparable(string1);
		String s2 = makeStringComparable(string2);

		String longer = s1, shorter = s2;
		if (s1.length() < s2.length()) {
			longer = s2;
			shorter = s1;
		}
		int longerLength = longer.length();
		if (longerLength == 0) {
			return 1.0;
		}

		return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
	}

	private int editDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0)
					costs[j] = j;
				else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}

	public Map<String, List<Pattern>> generateVariantsOfPatterns(List<String> words) {

		Map<String, List<Pattern>> wordPatterns = new HashMap<String, List<Pattern>>();

		for (String word : words) {
			if (!wordPatterns.containsKey(word)) {
				List<Pattern> patternsForBusinessPartner = convertStringToPatterns(word);
				wordPatterns.put(word, patternsForBusinessPartner);
			}
		}

		return wordPatterns;
	}

	public List<Pattern> convertStringToPatterns(String text) {
		return convertStringToPatterns(text, true);
	}

	public List<Pattern> convertStringToPatterns(String text, boolean useBoundaries) {
		List<String> resultPatternStrings = new ArrayList<String>();

		char[] textParts = text.toCharArray();

		int replaceCounter = 0;

		for (int i = 0; i < textParts.length; i++) {

			int characterCounter = 0;
			String resultPattern = "";
			for (char c : textParts) {
				String singlePart = String.valueOf(c);

				if (singlePart.equals(" ")) {
					resultPattern += "\\s{0,}";
				} else {

					String tmpSinglePart = singlePart;

					if (singlePart.toLowerCase().equals("i") || singlePart.toLowerCase().equals("l")) {
						tmpSinglePart = "il1";
					}

					String lowercaseSinglePart = tmpSinglePart.toLowerCase();
					String uppercaseSinglePart = tmpSinglePart.toUpperCase();

					if (lowercaseSinglePart.equals("&")) {
						lowercaseSinglePart = "\\" + lowercaseSinglePart;
						uppercaseSinglePart = "\\" + uppercaseSinglePart;
					}

					if (characterCounter == replaceCounter) {
						resultPattern += ".?";
					} else {
						resultPattern += "[" + lowercaseSinglePart + uppercaseSinglePart + "]{1}";
					}

					if (characterCounter < textParts.length - 1) {
						resultPattern += ".?";
					}
				}

				characterCounter++;

			}

			resultPatternStrings.add(resultPattern);
			replaceCounter++;
		}

		String newStringPattern = useBoundaries ? "\\b" : "";
		newStringPattern += "(";

		newStringPattern += StringUtils.join(resultPatternStrings, "|");

		newStringPattern += ")";
		newStringPattern += useBoundaries ? "\\b" : "";

		Pattern tmpPattern = Pattern.compile(newStringPattern);

		return Arrays.asList(tmpPattern);
	}

	public String makeStringComparable(String originalString) {
		String newString = originalString.replace(" ", "");
		newString = newString.toLowerCase();
		return newString;
	}
}
