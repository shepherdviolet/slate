/*
 * Copyright (C) 2015-2019 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/slate
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.x.monitor.txtimer;

import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import sviolet.slate.common.x.monitor.txtimer.def.DefaultTxTimerReportReader;
import sviolet.thistle.util.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TxTimerReportReaderTest {

    private static final String DATA1 = "" +
            "TxTimer | 0001 > last 2 min ( cnt:4, avg:1365ms, max:1442ms, min:1288ms ) total ( cnt:37, ing:0, est-avg:1466ms )\n" +
            "2019-09-16 16:09:09,933 d5cbec56ee4d4adba8534d286d450a9d per_rpc INFO Slate-TxTimer-Report-1 s.s.c.x.monitor.txtimer.def.Reporter flush 283: \n" +
            "TxTimer | ------------------------------------------------------------------------------------------------------------\n" +
            "TxTimer | Group (TestGroup) Time (2019-09-16 16:04:00 - 2019-09-16 16:09:00)  Page 1\n" +
            "TxTimer | 0001 > last 2 min ( cnt:2, avg:1265ms, max:1342ms, min:1188ms ) total ( cnt:37, ing:0, est-avg:1466ms )\n" +
            "TxTimer | 0002 > last 4 min ( cnt:8, avg:1062ms, max:1424ms, min:944ms ) total ( cnt:45, ing:0, est-avg:1014ms )\n" +
            "TxTimer | 0003 > last 0 min ( cnt:0, avg:0ms, max:0ms, min:0ms ) total ( cnt:2, ing:0, est-avg:2549ms )\n" +
            "TxTimer | 0004 > last 0 min ( cnt:0, avg:0ms, max:0ms, min:0ms ) total ( cnt:2, ing:0, est-avg:435ms )\n" +
            "2019-09-16 16:09:09,933 d5cbec56ee4d4adba8534d286d450a9d per_rpc INFO Slate-TxTimer-Report-1 s.s.c.x.monitor.txtimer.def.Reporter flush 283: \n" +
            "TxTimer | ------------------------------------------------------------------------------------------------------------\n" +
            "TxTimer | Group (TestGroup) Time (2019-09-16 16:04:00 - 2019-09-16 16:09:00)  Page 2\n" +
            "TxTimer | 0001 > last 5 min ( cnt:10, avg:1165ms, max:1242ms, min:1088ms ) total ( cnt:37, ing:0, est-avg:1466ms )";

    private static final String DATA2 = "" +
            "14:59:13.290 [Slate-TxTimer-Report-0] INFO sviolet.slate.common.x.monitor.txtimer.def.Reporter - \n" +
            "TxTimer | ------------------------------------------------------------------------------------------------------------\n" +
            "TxTimer | Group (HttpTransport) Time (2019-09-17 14:57:00 - 2019-09-17 14:58:00)  Page 1\n" +
            "TxTimer | Service1 > last 1 min ( cnt:97770, avg:95ms, max:1313ms, min:4ms ) total ( cnt:242996, ing:200, est-avg:95ms )\n" +
            "TxTimer | Service1 > last 1 min ( cnt:98903, avg:94ms, max:1308ms, min:3ms ) total ( cnt:246025, ing:200, est-avg:94ms )\n" +
            "TxTimer | Service2 > last 1 min ( cnt:100119, avg:92ms, max:1311ms, min:2ms ) total ( cnt:249177, ing:200, est-avg:92ms )\n" +
            "TxTimer | Service2 > last 1 min ( cnt:101202, avg:91ms, max:1295ms, min:1ms ) total ( cnt:251712, ing:200, est-avg:91ms )\n" +
            "TxTimer | Service3 > last 1 min ( cnt:102742, avg:90ms, max:1307ms, min:0ms ) total ( cnt:255383, ing:200, est-avg:90ms )\n" +
            "14:59:13.291 [Slate-TxTimer-Report-0] INFO sviolet.slate.common.x.monitor.txtimer.def.Reporter - \n" +
            "TxTimer | ------------------------------------------------------------------------------------------------------------\n" +
            "TxTimer | Group (HttpTransport) Time (2019-09-17 14:58:00 - 2019-09-17 14:59:00)  Page 1\n" +
            "TxTimer | Service1 > last 1 min ( cnt:120964, avg:99ms, max:904ms, min:4ms ) total ( cnt:242996, ing:200, est-avg:95ms )\n" +
            "TxTimer | Service2 > last 1 min ( cnt:122697, avg:97ms, max:910ms, min:3ms ) total ( cnt:246025, ing:200, est-avg:94ms )\n" +
            "TxTimer | Service2 > last 1 min ( cnt:124243, avg:96ms, max:904ms, min:2ms ) total ( cnt:249177, ing:200, est-avg:92ms )\n" +
            "TxTimer | Service3 > last 1 min ( cnt:125431, avg:95ms, max:899ms, min:1ms ) total ( cnt:251712, ing:200, est-avg:91ms )\n" +
            "TxTimer | Service4 > last 1 min ( cnt:127251, avg:94ms, max:900ms, min:0ms ) total ( cnt:255383, ing:200, est-avg:90ms )";

    private static final String EXPECTED = "{HttpTransport={Service4={1568703480000=Record{group='HttpTransport', name='Service4', time=1568703480000, count=127251.0, avgElapse=94.0, maxElapse=900, minElapse=0}}, Service3={1568703420000=Record{group='HttpTransport', name='Service3', time=1568703420000, count=102742.0, avgElapse=90.0, maxElapse=1307, minElapse=0}, 1568703480000=Record{group='HttpTransport', name='Service3', time=1568703480000, count=125431.0, avgElapse=95.0, maxElapse=899, minElapse=1}}, Service2={1568703420000=Record{group='HttpTransport', name='Service2', time=1568703420000, count=201321.0, avgElapse=91.49731026569509, maxElapse=1311, minElapse=1}, 1568703480000=Record{group='HttpTransport', name='Service2', time=1568703480000, count=246940.0, avgElapse=96.4968696849437, maxElapse=910, minElapse=2}}, Service1={1568703420000=Record{group='HttpTransport', name='Service1', time=1568703420000, count=196673.0, avgElapse=94.49711958428458, maxElapse=1313, minElapse=3}, 1568703480000=Record{group='HttpTransport', name='Service1', time=1568703480000, count=120964.0, avgElapse=99.0, maxElapse=904, minElapse=4}}}, TestGroup={0002={1568621040000=Record{group='TestGroup', name='0002', time=1568621040000, count=1.6, avgElapse=1062.0, maxElapse=1424, minElapse=944}, 1568621100000=Record{group='TestGroup', name='0002', time=1568621100000, count=1.6, avgElapse=1062.0, maxElapse=1424, minElapse=944}, 1568621160000=Record{group='TestGroup', name='0002', time=1568621160000, count=1.6, avgElapse=1062.0, maxElapse=1424, minElapse=944}, 1568621220000=Record{group='TestGroup', name='0002', time=1568621220000, count=1.6, avgElapse=1062.0, maxElapse=1424, minElapse=944}, 1568621280000=Record{group='TestGroup', name='0002', time=1568621280000, count=1.6, avgElapse=1062.0, maxElapse=1424, minElapse=944}}, 0001={1568621040000=Record{group='TestGroup', name='0001', time=1568621040000, count=2.4, avgElapse=1181.6666666666667, maxElapse=1342, minElapse=1088}, 1568621100000=Record{group='TestGroup', name='0001', time=1568621100000, count=2.4, avgElapse=1181.6666666666667, maxElapse=1342, minElapse=1088}, 1568621160000=Record{group='TestGroup', name='0001', time=1568621160000, count=2.4, avgElapse=1181.6666666666667, maxElapse=1342, minElapse=1088}, 1568621220000=Record{group='TestGroup', name='0001', time=1568621220000, count=2.4, avgElapse=1181.6666666666667, maxElapse=1342, minElapse=1088}, 1568621280000=Record{group='TestGroup', name='0001', time=1568621280000, count=2.4, avgElapse=1181.6666666666667, maxElapse=1342, minElapse=1088}}}}";

    @Test
    public void testReportReader() throws IOException {

        FileUtils.writeString(new File("./out/test-case/txtimer/data1.stat"), DATA1, false);
        FileUtils.writeString(new File("./out/test-case/txtimer/unexpected.txt"), "unexpected data", false);
        FileUtils.writeString(new File("./out/test-case/txtimer/dir2/data2.stat"), DATA2, false);

        DefaultTxTimerReportReader reader = DefaultTxTimerReportReader.newReader()
                .read("./out/test-case/txtimer/", ".*\\.stat$", true, StandardCharsets.UTF_8);

//        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(reader.getAllRecords()));

        Assert.assertEquals(
                EXPECTED,
                reader.getAllRecords().toString());

    }

}
