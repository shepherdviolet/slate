package sviolet.slate.common.utilx.txtimer;

import sviolet.thistle.util.crypto.SecureRandomUtils;

public class TxTimerCommonTest {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("slate.txtimer.enabled", "true");
        System.setProperty("slate.txtimer.reportinterval", "2");

        for (int i = 0 ; i < 1000 ; i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException ignored) {
                    }

                    long startTime = System.currentTimeMillis();
                    while (System.currentTimeMillis() - startTime < 10 * 60 * 1000L) {

                        try {
                            TxTimer.start("HttpTransport", "Service" + String.valueOf(finalI % 50));
                            Thread.sleep(SecureRandomUtils.nextInt(100) + finalI % 100);
//                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        } finally {
                            TxTimer.stop();
                        }

                    }

                }
            }).start();
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 12 * 60 * 1000L) {
            Thread.sleep(10000L);
        }
    }

}
