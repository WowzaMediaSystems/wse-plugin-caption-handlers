/*
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 *  © 2024 Wowza Media Systems, LLC. All rights reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of Wowza Media Systems, LLC.
 * The intellectual and technical concepts contained herein are proprietary to Wowza Media Systems, LLC
 * and may be covered by U.S. and Foreign Patents, patents in process, and are protected by trade secret
 * or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from Wowza Media Systems, LLC. Access to the source code
 * contained herein is hereby forbidden to anyone except current Wowza Media Systems, LLC employees, managers
 * or contractors who have executed Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure of this
 * source code, which includes information that is confidential and/or proprietary, and is a trade secret, of
 * Wowza Media Systems, LLC. ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF WOWZA MEDIA SYSTEMS, LLC IS
 * STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION
 * OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR
 * DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */

package com.wowza.wms.plugin.captions.caption;

import com.wowza.wms.plugin.captions.ModuleCaptionsBase;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CaptionHelperTest
{

	static String firstPassTerminators = ModuleCaptionsBase.DEFAULT_FIRST_PASS_TERMINATORS;
	static int firstPassPercentage = ModuleCaptionsBase.DEFAULT_FIRST_PASS_PERCENTAGE;

	@Test
	void testGetMultiLineCaptionsReturnsMultipleCaptions()
	{
		String text = "Our greatest weakness lies in giving up. The most certain way to succeed is always to try just one more time.";
		long offsetVal = 270365;
		BigInteger offset = BigInteger.valueOf(offsetVal * 10000);
		long durationVal = 3030;
		BigInteger duration = BigInteger.valueOf(durationVal * 10000);
		Instant start = CaptionHelper.epochInstantFromTicks(offset);
		Instant end = CaptionHelper.epochInstantFromTicks(offset.add(duration));
		String language = "eng";
		int maxLineLength = 32;
		int maxLineCount = 3;
		CaptionTiming captionTiming = new CaptionTiming(start, end);
		List<Caption> captions = CaptionHelper.getCaptions(Locale.forLanguageTag(language).getLanguage(), maxLineLength, maxLineCount,
				firstPassTerminators, firstPassPercentage, captionTiming, text);
		assertEquals(2, captions.size());
		System.out.println("captions = " + captions);
		Duration perLineDuration = Duration.between(start, end).dividedBy(4);

		Caption caption = captions.get(0);
		long startMillis = offsetVal;
		long endMillis = offsetVal + (long)((durationVal / 4.0) * 3);
		assertEquals("Our greatest weakness lies in\ngiving up. The most certain way\nto succeed is always to try just", caption.getText());
		assertEquals(startMillis, caption.getBegin());
		assertEquals(endMillis, caption.getEnd());

		caption = captions.get(1);
		startMillis = endMillis;
		endMillis = offsetVal + durationVal;
		assertEquals("one more time.", caption.getText());
		assertEquals(startMillis, caption.getBegin());
		assertEquals(endMillis, caption.getEnd());
	}

	@Test
	void testGetMultiLineCaptionsWithoutPunctuation()
	{
		String text = "art and science and infusing all of that data into storytelling";
		long offsetVal = 270365;
		BigInteger offset = BigInteger.valueOf(offsetVal * 10000);
		long durationVal = 3030;
		BigInteger duration = BigInteger.valueOf(durationVal * 10000);
		Instant start = CaptionHelper.epochInstantFromTicks(offset);
		Instant end = CaptionHelper.epochInstantFromTicks(offset.add(duration));
		String language = "eng";
		int maxLineLength = 32;
		int maxLineCount = 3;
		CaptionTiming captionTiming = new CaptionTiming(start, end);
		List<Caption> captions = CaptionHelper.getCaptions(Locale.forLanguageTag(language).getLanguage(), maxLineLength, maxLineCount,
				firstPassTerminators, firstPassPercentage, captionTiming, text);
		System.out.println("captions = " + captions);
		assertEquals("art and science and infusing all\nof that data into storytelling", captions.get(0).getText());
	}

	@Test
	void testGetMultiLineCaptionsWithPunctuation()
	{
		String text = "our attendees, this year. So here at Rising, we are joined";
		long offsetVal = 270365;
		BigInteger offset = BigInteger.valueOf(offsetVal * 10000);
		long durationVal = 3030;
		BigInteger duration = BigInteger.valueOf(durationVal * 10000);
		Instant start = CaptionHelper.epochInstantFromTicks(offset);
		Instant end = CaptionHelper.epochInstantFromTicks(offset.add(duration));
		String language = "eng";
		int maxLineLength = 32;
		int maxLineCount = 3;
		CaptionTiming captionTiming = new CaptionTiming(start, end);
		List<Caption> captions = CaptionHelper.getCaptions(Locale.forLanguageTag(language).getLanguage(), maxLineLength, maxLineCount,
				firstPassTerminators, firstPassPercentage, captionTiming, text);
		System.out.println("captions = " + captions);
		assertEquals("our attendees, this year.\nSo here at Rising, we are joined", captions.get(0).getText());
	}

	@Test
	void testGetMultiLineCaptionsWithLatePunctuation()
	{
		String text = "Quality is not an act, it is a habit.";
		long offsetVal = 270365;
		BigInteger offset = BigInteger.valueOf(offsetVal * 10000);
		long durationVal = 3030;
		BigInteger duration = BigInteger.valueOf(durationVal * 10000);
		Instant start = CaptionHelper.epochInstantFromTicks(offset);
		Instant end = CaptionHelper.epochInstantFromTicks(offset.add(duration));
		String language = "eng";
		int maxLineLength = 32;
		int maxLineCount = 3;
		CaptionTiming captionTiming = new CaptionTiming(start, end);
		List<Caption> captions = CaptionHelper.getCaptions(Locale.forLanguageTag(language).getLanguage(), maxLineLength, maxLineCount,
				firstPassTerminators, firstPassPercentage, captionTiming, text);
		System.out.println("captions = " + captions);
		assertEquals("Quality is not an act,\nit is a habit.", captions.get(0).getText());
	}

	@Test
	void testGetMultiLineCaptionsWithDomainNameSplitsCorrectly()
	{
		String text = "This caption contains example.com";
		long offsetVal = 270365;
		BigInteger offset = BigInteger.valueOf(offsetVal * 10000);
		long durationVal = 3030;
		BigInteger duration = BigInteger.valueOf(durationVal * 10000);
		Instant start = CaptionHelper.epochInstantFromTicks(offset);
		Instant end = CaptionHelper.epochInstantFromTicks(offset.add(duration));
		String language = "eng";
		int maxLineLength = 32;
		int maxLineCount = 3;
		CaptionTiming captionTiming = new CaptionTiming(start, end);
		List<Caption> captions = CaptionHelper.getCaptions(Locale.forLanguageTag(language).getLanguage(), maxLineLength, maxLineCount,
				firstPassTerminators, firstPassPercentage, captionTiming, text);
		System.out.println("captions = " + captions);
		assertEquals("This caption contains\nexample.com", captions.get(0).getText());
	}

	@Test
	void testPeriodAtMaxLengthSplitsCorrectly()
	{
		String text = "abcde fgjij klmno pqrst uvw xyz. abcde fgjij klmno pqrst uvw xyz.";
		long offsetVal = 270365;
		BigInteger offset = BigInteger.valueOf(offsetVal * 10000);
		long durationVal = 3030;
		BigInteger duration = BigInteger.valueOf(durationVal * 10000);
		Instant start = CaptionHelper.epochInstantFromTicks(offset);
		Instant end = CaptionHelper.epochInstantFromTicks(offset.add(duration));
		String language = "eng";
		int maxLineLength = 32;
		int maxLineCount = 3;
		CaptionTiming captionTiming = new CaptionTiming(start, end);
		List<Caption> captions = CaptionHelper.getCaptions(Locale.forLanguageTag(language).getLanguage(), maxLineLength, maxLineCount,
				firstPassTerminators, firstPassPercentage, captionTiming, text);
		System.out.println("captions = " + captions);
		assertEquals("abcde fgjij klmno pqrst uvw xyz.\nabcde fgjij klmno pqrst uvw xyz.", captions.get(0).getText());
	}

	@Test
	void testMultipleSpeakersSplitsCorrectly()
	{
		String text = ">>> Hello. >>> Hi there. >>> How are you? >>> I'm good, thanks.";
		long offsetVal = 270365;
		BigInteger offset = BigInteger.valueOf(offsetVal * 10000);
		long durationVal = 3030;
		BigInteger duration = BigInteger.valueOf(durationVal * 10000);
		Instant start = CaptionHelper.epochInstantFromTicks(offset);
		Instant end = CaptionHelper.epochInstantFromTicks(offset.add(duration));
		String language = "eng";
		int maxLineLength = 32;
		int maxLineCount = 4;
		CaptionTiming captionTiming = new CaptionTiming(start, end);
		List<Caption> captions = CaptionHelper.getCaptions(Locale.forLanguageTag(language).getLanguage(), maxLineLength, maxLineCount,
				firstPassTerminators, firstPassPercentage, ">>>", captionTiming, text);
		System.out.println("captions = " + captions);
		assertEquals(">>> Hello.\n>>> Hi there.\n>>> How are you?\n>>> I'm good, thanks.", captions.get(0).getText());
	}

	@Test
	void testMultipleSpeakersWithLongLinesSplitsCorrectly()
	{
		String text = "Hi there. How are doing you today? >>> I'm good, thanks. What about you?";
		long offsetVal = 270365;
		BigInteger offset = BigInteger.valueOf(offsetVal * 10000);
		long durationVal = 3030;
		BigInteger duration = BigInteger.valueOf(durationVal * 10000);
		Instant start = CaptionHelper.epochInstantFromTicks(offset);
		Instant end = CaptionHelper.epochInstantFromTicks(offset.add(duration));
		String language = "eng";
		int maxLineLength = 32;
		int maxLineCount = 4;
		CaptionTiming captionTiming = new CaptionTiming(start, end);
		List<Caption> captions = CaptionHelper.getCaptions(Locale.forLanguageTag(language).getLanguage(), maxLineLength, maxLineCount,
				firstPassTerminators, firstPassPercentage, ">>>", captionTiming, text);
		System.out.println("captions = " + captions);
		assertEquals("Hi there. How are doing you\ntoday?\n>>> I'm good, thanks.\nWhat about you?", captions.get(0).getText());
	}
}