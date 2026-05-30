import unittest

from observer import parse_ss


class ObserverParserTest(unittest.TestCase):
    def test_parses_host_ss_rows(self):
        rows = [
            "Netid State Recv-Q Send-Q Local Address:Port Peer Address:Port Process",
            'tcp ESTAB 0 0 24.199.87.150:22 71.251.0.115:64373 users:(("sshd",pid=944,fd=3))',
            'udp UNCONN 0 0 0.0.0.0:443 0.0.0.0:* users:(("docker-proxy",pid=1036965,fd=8))',
            'tcp LISTEN 0 4096 *:8088 *:* users:(("java",pid=1,fd=21))',
        ]

        snapshots = parse_ss(rows)

        self.assertEqual(3, len(snapshots))
        self.assertEqual("TCP", snapshots[0]["protocol"])
        self.assertEqual("24.199.87.150", snapshots[0]["localIp"])
        self.assertEqual(22, snapshots[0]["localPort"])
        self.assertEqual("71.251.0.115", snapshots[0]["remoteIp"])
        self.assertEqual(64373, snapshots[0]["remotePort"])
        self.assertEqual("ESTABLISHED", snapshots[0]["state"])
        self.assertEqual(944, snapshots[0]["pid"])
        self.assertIsNone(snapshots[1]["remoteIp"])
        self.assertIsNone(snapshots[1]["remotePort"])
        self.assertEqual("0.0.0.0", snapshots[2]["localIp"])
        self.assertEqual("LISTENING", snapshots[2]["state"])


if __name__ == "__main__":
    unittest.main()
