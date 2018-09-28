package sviolet.slate.common.utilx.txtimer;

import sviolet.thistle.util.crypto.SecureRandomUtils;

public class TxTimerCommonTest {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("slate.txtimer.enabled", "true");
        System.setProperty("slate.txtimer.reportinterval", "3");

        for (int i = 0 ; i < 100 ; i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    while (System.currentTimeMillis() - startTime < 20 * 60 * 1000L) {
                        try {
                            TxTimer.start("Test", String.valueOf(finalI % 10));
                            Thread.sleep(SecureRandomUtils.nextInt(100) + finalI);
                        } catch (InterruptedException ignored) {
                        } finally {
                            TxTimer.stop();
                        }
                    }
                }
            }).start();
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 30 * 60 * 1000L) {
            Thread.sleep(10000L);
        }
    }

}
