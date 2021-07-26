import common.utils.IpUtils;
import org.junit.Test;

public class IpUtilsTest {

    @Test
    public void testIsIp() {
        boolean t1 = IpUtils.isIp("192,168.1.1");
        boolean t2 = IpUtils.isIp("1.0.0.0");
        boolean t3 = IpUtils.isIp("192.168.1.256");
        boolean t4 = IpUtils.isIp("0.168.1.1");
        System.out.println("result:" + t1 + t2 + t3 + t4);
    }
}
