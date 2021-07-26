package common.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class IpUtils {

    private static final String REGEXP_IP_OTHER = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

    public static Pattern pattern = Pattern.compile(REGEXP_IP_OTHER);

    /**
     * ip格式校验
     */
    public static boolean isIp(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        return pattern.matcher(ip).matches();
    }


    /**
     * 将ip转换为long类型
     * @param ip  192.168.1.1
     * @return  11111111L
     */
    public static Long transIpToLong(String ip) {
        if (!isIp(ip)) {
            return null;
        }
        long[] longIp = new long[4];
        int position1 = ip.indexOf(".");
        int position2 = ip.indexOf(".", position1 + 1);
        int position3 = ip.indexOf(".", position2 + 1);
        longIp[0] = Long.parseLong(ip.substring(0, position1));
        longIp[1] = Long.parseLong(ip.substring(position1 + 1, position2));
        longIp[2] = Long.parseLong(ip.substring(position2 + 1, position3));
        longIp[3] = Long.parseLong(ip.substring(position3 + 1));
        return (longIp[0] << 24) + (longIp[1] << 16) + (longIp[2] << 8) + longIp[3];
    }

    /**
     * 将ip转换为String类型
     * @param ip 11111111
     * @return 192.168.1.1
     */
    public static String transIpToString(Long ip) {
        if (ip == null) {
            return null;
        }
        return (ip >>> 24) + "." + ((ip & 0x00FFFFFF) >>> 16) + "." + ((ip & 0x0000FFFF) >>> 8) + "." + (ip & 0x000000FF);
    }

    /**
     * 将long类型起止ip段转换为string类型
     * @param start 1111111
     * @param end   2222222
     * @return  192.169.1.1-192.168.2.2
     */
    public static String mergeStartEndIp(Long start, Long end) {
        if (start.equals(end)) {
            return transIpToString(end);
        } else {
            return transIpToString(start) + "-" + transIpToString(end);
        }
    }

    /**
     * 将ip转换为起止ip形式
     * 例如: 输入 192.168.1.1-192.168.1.100 输出 [11111001,11111100]
     * 例如: 输入 192.168.1.1 输出 [11111001,11111001]
     * @param ip   ip或ip-ip的形式
     * @return  小的在前,大的在后
     */
    public static List<Long> changeIpToStartIpAndEndIp(String ip) {
        if (StringUtils.isEmpty(ip)) {
            throw new RuntimeException("输入不能为空");
        }
        String startIpStr;
        String endIpStr;
        if (ip.contains("-")) {
            String[] ips = ip.split("-");
            if (ips.length > 2) {
                throw new RuntimeException("不支持的格式");
            }
            startIpStr = ips[0];
            endIpStr = ips[1];
        } else {
            startIpStr = endIpStr = ip;
        }

        if (!isIp(startIpStr) || !isIp(endIpStr)) {
            throw new RuntimeException("ip格式错误");
        }

        Long startIp = transIpToLong(startIpStr);
        Long endIp = transIpToLong(endIpStr);

        if (startIp == null || endIp == null) {
            throw new RuntimeException("ip格式错误");
        }

        if (startIp > endIp) {
            startIp = startIp ^ endIp;
            endIp = startIp ^ endIp;
            startIp = startIp ^ endIp;
        }
        List<Long> startIpAndEndIp = new ArrayList<>();
        startIpAndEndIp.add(startIp);
        startIpAndEndIp.add(endIp);
        return startIpAndEndIp;
    }

    /**
     * 检查ip列表里是否有重复数据
     * @param strArray   [192.168.1.1,192.168.1.2,192.168.1.1-192.168.1.5]
     * @return true表示有重复数据 false表示无重复数据
     */
    public static boolean checkHasRepeatIp(String[] strArray) {
        if (strArray.length == 0) {
            return false;
        }
        List<Long[]> ipList = new ArrayList<>();
        for (String str : strArray) {
            String startIpTmp;
            String endIpTmp;
            if (str.contains("-")) {
                String[] strTmp = str.split("-");
                if (strTmp.length != 2 || !isIp(strTmp[0]) || !isIp(strTmp[1])) {
                    throw new RuntimeException("网段输入有误!");
                }
                startIpTmp = strTmp[0];
                endIpTmp = strTmp[1];
            } else {
                if (!isIp(str)) {
                    throw new RuntimeException("网段输入有误!");
                }
                startIpTmp = endIpTmp = str;

            }
            Long startIp = transIpToLong(startIpTmp);
            Long endIp = transIpToLong(endIpTmp);
            if (startIp == null || endIp == null) {
                throw new RuntimeException("网段输入有误!");
            }
            if (startIp > endIp) {
                startIp = startIp ^ endIp;
                endIp = startIp ^ endIp;
                startIp = startIp ^ endIp;
            }
            if (!CollectionUtils.isEmpty(ipList)) {
                for (Long[] longs : ipList) {
                    if (Math.max(longs[0], startIp) <= Math.min(longs[1], endIp)) {
                        return true;
                    }
                }
            }
            Long[] ipArray = {startIp, endIp, Math.abs(startIp - endIp)};
            ipList.add(ipArray);
        }
        return false;
    }

    /**
     * 贪心聚合ip段
     * @param strArray  String[] arr = {192.168.1.1,192.168.1.2,192.168.1.3,192.168.1.4-192.168.1.5}
     * @return  {192.168.1.1-192.168.1.6}
     */
    public static String[] aggregationIpSegment(String[] strArray) {
        String[] result;
        long[][] intervals = new long[strArray.length][2];
        for (int i = 0; i < strArray.length; i++) {
            String str = strArray[i];
            if (str.contains("-")) {
                String[] strTmp = str.split("-");
                Long startIp = transIpToLong(strTmp[0]);
                Long endIp = transIpToLong(strTmp[1]);
                if (startIp == null || endIp == null) {
                    continue;
                }
                if (startIp > endIp) {
                    startIp = startIp ^ endIp;
                    endIp = startIp ^ endIp;
                    startIp = startIp ^ endIp;
                }
                intervals[i][0] = startIp;
                intervals[i][1] = endIp;

            } else {
                Long transIpToLong = transIpToLong(str);
                if (transIpToLong == null) {
                    continue;
                }
                intervals[i][0] = transIpToLong;
                intervals[i][1] = transIpToLong;
            }
        }
        int len = intervals.length;
        if (len < 2) {
            return strArray;
        }
        Arrays.sort(intervals, Comparator.comparingLong(o -> o[0]));
        List<long[]> res = new ArrayList<>();
        res.add(intervals[0]);

        for (int i = 1; i < len; i++) {
            long[] curInterval = intervals[i];
            long[] peek = res.get(res.size() - 1);
            if (curInterval[0] > peek[1] + 1) {
                res.add(curInterval);
            } else {
                peek[1] = Math.max(curInterval[1], peek[1]);
            }
        }
        long[][] pack = res.toArray(new long[res.size()][]);
        result = new String[pack.length];
        for (int i = 0; i < pack.length; i++) {
            if (pack[i][0] == pack[i][1]) {
                result[i] = transIpToString(pack[i][0]);
            } else {
                result[i] = transIpToString(pack[i][0]) + "-" + transIpToString(pack[i][1]);
            }
        }
        return result;
    }

    /**
     * 将IP段分片
     * @param strArray ip段
     * @return 片
     */
    private String[] replaceIpTarget(String[] strArray, int shardNum) {
        List<long[]> ipList = new ArrayList<>();
        for (String str : strArray) {
            String startIpTmp;
            String endIpTmp;
            if (str.contains("-")) {
                String[] strTmp = str.split("-");
                startIpTmp = strTmp[0];
                endIpTmp = strTmp[1];
            } else {
                startIpTmp = endIpTmp = str;
            }
            Long startIp = transIpToLong(startIpTmp);
            Long endIp = transIpToLong(endIpTmp);
            if (startIp == null || endIp == null) {
                continue;
            }
            if (startIp > endIp) {
                startIp = startIp ^ endIp;
                endIp = startIp ^ endIp;
                startIp = startIp ^ endIp;
            }
            long[] longs = {startIp, endIp};
            ipList.add(longs);
        }
        String[] shardIpTarget = new String[shardNum];
        for (int i = 0; i < ipList.size(); i++) {
            long[] longs = ipList.get(i);
            List<long[]> list = averageIpTarget(longs, shardNum);
            for (long[] ipSegment : list) {
                //有序散列
                int j = i % shardNum;
                if (StringUtils.isEmpty(shardIpTarget[j])) {
                    shardIpTarget[j] = "";
                }
                shardIpTarget[j] = shardIpTarget[j] + transIpToString(ipSegment[0]) + "-" + transIpToString(ipSegment[1]) + ",";

            }
        }
        return shardIpTarget;
    }

    /**
     * 均匀分片
     * @param idShard   ip区间
     * @param n         片数
     * @return          分片后结果
     */
    public static List<long[]> averageIpTarget(long[] idShard, long n) {
        List<long[]> result = new ArrayList<>();
        long size = idShard[1] - idShard[0] + 1;
        long remainder = size % n;
        long number = size / n;
        long offset = 0;
        for (int i = 0; i < n; i++) {
            long[] shard = new long[2];
            shard [0] = idShard[0] + i * number + offset;
            if (shard[0] > idShard[1]) {
                break;
            }
            shard [1] = idShard[0] + (i + 1) * number + offset - 1;
            if (remainder > 0) {
                shard[1] = shard[1] + 1;
                remainder--;
                offset++;
            }
            result.add(shard);
        }
        return result;
    }
}
