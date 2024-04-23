package com.bosum.gateway.mq.utils;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 时间工具类
 *
 * @author ruoyi
 */
public class DateUtils {
    public static String YYYY = "yyyy";

    public static String YYYY_MM = "yyyy-MM";

    public static String YYYY_MM_DD = "yyyy-MM-dd";

    public static String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static String YYYYMMDD = "yyyyMMdd";

    public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    public static String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";

    public static String YYYYMM = "yyyyMM";

    public static String DD="dd";

    public static String[] parsePatterns = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};


    public static Date getDateHOUR(Date date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, +8); //时间加8个小时
        return  calendar.getTime();
    }

    public static Date getDate(Date date)
    {
        try
        {
            return new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS).parse(DateUtil.format(date, "yyyy-MM-dd HH:mm:ss"));
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }

    }

    public static String getDateString(Date date)
    {
        return  DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 获取当前Date型日期 减去一秒
     *
     */
    public static Date getDate_seconds(Date date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, +1); // 减去1秒
        return  calendar.getTime();
    }

    /**
     * 获取当前Date型日期
     *
     * @return Date() 当前日期
     */
    public static Date getNowDate()
    {
        return new Date();
    }

    /**
     * 获取当前日期, 默认格式为yyyy-MM-dd
     *
     * @return String
     */
    public static String getDate()
    {
        return dateTimeNow(YYYY_MM_DD);
    }

    public static final String getTime()
    {
        return dateTimeNow(YYYY_MM_DD_HH_MM_SS);
    }

    public static final String getDateYYYY_MM_DD_HH_MM_SS(Date date)
    {
        return parseDateToStr(YYYY_MM_DD_HH_MM_SS,date);
    }

    public static final String dateTimeNow()
    {
        return dateTimeNow(YYYYMMDDHHMMSS);
    }

    public static final String dateTimeNow(Date date)
    {
        return dateTimeNow(YYYYMMDDHHMMSS,date);
    }

    public static final String dateTimeNow(final String format)
    {
        return parseDateToStr(format, new Date());
    }

    public static final String dateTimeNow(final String format,Date date)
    {
        return parseDateToStr(format,date);
    }


    public static final String dateTimeNows(Date date)
    {
        return dateTimeNows(YYYYMMDD,date);
    }

    public static final String dateTimeNows(final String format,Date date)
    {
        return parseDateToStr(format, date);
    }


    public static final String dateTime(final Date date)
    {
        return parseDateToStr(YYYY_MM_DD, date);
    }

    public static final String parseDateToStr(final String format, final Date date)
    {
        return new SimpleDateFormat(format).format(date);
    }

    public static final Date dateTime(final String format, final String ts)
    {
        try
        {
            return new SimpleDateFormat(format).parse(ts);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取服务器启动时间
     */
    public static Date getServerStartDate()
    {
        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new Date(time);
    }

    /**
     * 计算两个时间差
     */
    public static String getDatePoor(Date endDate, Date nowDate)
    {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return day + "天" + hour + "小时" + min + "分钟";
    }

    /**
     * 计算两个时间相差几天
     */
    public static long getDifferDay(Date endDate, Date startDate)
    {
        long nd = 1000 * 24 * 60 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - startDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        return day;
    }

    /**
     * 增加 LocalDateTime ==> Date
     */
    public static Date toDate(LocalDateTime temporalAccessor)
    {
        ZonedDateTime zdt = temporalAccessor.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * 增加 LocalDate ==> Date
     */
    public static Date toDate(LocalDate temporalAccessor)
    {
        LocalDateTime localDateTime = LocalDateTime.of(temporalAccessor, LocalTime.of(0, 0, 0));
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /***
     * 求开始至结束月份集合
     * @param minDate
     * @param maxDate
     * @return
     * @throws Exception
     */
    public static List<String> getMonthBetween(Date minDate, Date maxDate, String format) throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat(format);//格式化为年月

        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();

        min.setTime(minDate);
        min.set(min.get(Calendar.YEAR), min.get(Calendar.MONTH), 1);

        max.setTime(maxDate);
        max.set(max.get(Calendar.YEAR), max.get(Calendar.MONTH), 2);

        Calendar curr = min;
        while (curr.before(max)) {
            result.add(sdf.format(curr.getTime()));
            curr.add(Calendar.MONTH, 1);
        }

        return result;
    }

    /***
     * 求开始至结束天份集合
     * @param minDate
     * @param maxDate
     * @return
     * @throws Exception
     */
    public static List<String> getDayBetween(Date minDate, Date maxDate, String format) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(format);//格式化为年月
        String start=sdf.format(minDate);
        String end=sdf.format(maxDate);
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        long between = ChronoUnit.DAYS.between(startDate, endDate);
        List<String> dayList=new ArrayList<>();
        if (between < 1) {
            dayList.addAll(Stream.of(start, end).collect(Collectors.toSet()));
            Collections.reverse(dayList);
            return dayList;
        }
        dayList.addAll( Stream.iterate(startDate, e -> e.plusDays(1))

                .limit(between + 1)

                .map(LocalDate::toString)

                .collect(Collectors.toSet()));
        Collections.reverse(dayList);
        return dayList;
    }


    /***
     * 根据年月获取月末最后一天日期
     * @param date
     * @return
     */
    public static Date getLastDay(Date date) {
        Calendar cale = Calendar.getInstance();
        cale.setTime(date);
        int lastDay = cale.getActualMaximum(Calendar.DAY_OF_MONTH);//获取月最大天数
        cale.set(Calendar.DAY_OF_MONTH, lastDay);//设置日历中月份的最大天数
        return  cale.getTime();
    }


    /***
     *  Date1.before(Date2)，当begTime 小于endTime时，返回TRUE，当大于等于时，返回false；
     * @return
     */
    public static Boolean DateComparison(String begTime,String endTime)  {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date begDate = format.parse(begTime);
            Date endDate = format.parse(endTime);
            return begDate.before(endDate);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 判断一个日期是否是本月
     * @param time
     * @return
     * @throws ParseException
     */
    public static boolean isThisTime(Date time) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        Date date =time;
        String param = sdf.format(date);//参数时间
        String now = sdf.format(new Date());//当前时间
        if (param.equals(now)) {
            return true;
        }
        return false;
    }


    /**
     * 通过日期 加上多少个小时 返回时间
     */
    public static Date addHour_Of_Day(Date time,int hour)  {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.HOUR_OF_DAY, hour);
        Date newDate = cal.getTime();
        return  newDate;
    }


    public static void main(String[] args) throws ParseException {
        Date data1 = DateUtil.parse("2023-08-15 12:24:27");
        Date date2 = DateUtil.parse("2023-08-15 12:23:28");

        System.out.println("两个时间相差 " + DateUtil.between(data1, date2, DateUnit.DAY) + " 天");
        System.out.println("两个时间相差 " + DateUtil.between(data1, date2, DateUnit.HOUR) + " 小时");
        System.out.println("两个时间相差 " + (DateUtil.between(data1, date2, DateUnit.MINUTE)) + " 分钟");
        System.out.println("两个时间相差 " + DateUtil.between(data1, date2, DateUnit.SECOND) + " 秒");
        System.out.println("两个时间相差 " + DateUtil.between(data1, date2, DateUnit.MS) + " 毫秒");

        String begTime = "2023-11-01 00:00:00";
        String endTime = "2020-05-27 12:01:01";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date begDate = format.parse(begTime);
        Date endDate = format.parse(endTime);
        //System.out.println(begDate.before(endDate));
        System.out.println(begDate.before(new Date()));


        SimpleDateFormat format2 = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar nowTime = Calendar.getInstance();
        nowTime.add(Calendar.MINUTE, 5);

        System.out.println("当前时间加五分钟" + format2.format(new Date()));
        System.out.println("当前时间加五分钟" + format2.format(nowTime.getTime()));
        long currentTimeMillis = System.currentTimeMillis();


        Date date = new Date();
        System.out.println("原始日期：" + date);

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR_OF_DAY, 24);

        Date newDate = cal.getTime();
        SimpleDateFormat format3 = new SimpleDateFormat("yyyyMMdd");
        System.out.println("添加24小时后的日期：" +  format3.format(newDate));


        if(new BigDecimal(0).compareTo(new BigDecimal("0")) >0){
            System.out.println(true);
        }



    }



}
