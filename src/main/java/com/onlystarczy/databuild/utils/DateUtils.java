package com.onlystarczy.databuild.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

public class DateUtils {

	private static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
	private static final ThreadLocal<SimpleDateFormat> threadLocalDateFormat = ThreadLocal
			.withInitial(() -> new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS, Locale.US));
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);

	/**
	 * SimpleDateFormat要结合ThreadLocal使用,避免并发问题
	 */
	public static String formatBySimpleDateFormat(Date date, String... pattern) {
		SimpleDateFormat simpleDateFormat = threadLocalDateFormat.get();
		if (pattern.length > 0) {
			simpleDateFormat.applyPattern(pattern[0]);
		}
		return simpleDateFormat.format(date);
	}

	/**
	 * java8新增的方法
	 */
	public static String formatByDateTimeFormatter(Date date, String... pattern) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		if (pattern.length > 0) {
			return DateTimeFormatter.ofPattern(pattern[0]).withZone(ZoneId.systemDefault()).format(localDateTime);
		}
		return dateTimeFormatter.format(localDateTime);
	}
	
	public static Date parse(String dateStr, String... pattern) {
		SimpleDateFormat simpleDateFormat = threadLocalDateFormat.get();
		if (pattern.length > 0) {
			simpleDateFormat.applyPattern(pattern[0]);
		}
		try {
			return simpleDateFormat.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 阿帕奇的方法
	 */
	public static String formatApacheDateFormatUtils(Date date, String... pattern) {
		if (pattern.length > 0) {
			return DateFormatUtils.format(date, pattern[0]);
		}
		return DateFormatUtils.format(date, YYYY_MM_DD_HH_MM_SS);
	}
	
	public static Date randomTime(String startTime, String endTime) {
		return randomTime(startTime, endTime, null);
	}

	public static Date randomTime(String startTime, String endTime, String pattern) {
		SimpleDateFormat sdf = null;
		if(StringUtils.isBlank(pattern)) {
			sdf = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
		}else {
			sdf = new SimpleDateFormat(pattern);
		}
		
		Date startDate = null;
		Date endDate = null;
		try {
			startDate = sdf.parse(startTime);
			endDate = sdf.parse(endTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		long startTimeSecond = (endDate.getTime() - startDate.getTime()) / 1000;
		int random = (int) (Math.random() * startTimeSecond);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		calendar.add(Calendar.SECOND, random);
		return calendar.getTime();
	}

}
