#/bin/sh

# log='/dev/null'
log="log.txt"
count=`echo "(1024*1024*1024*$1)/(1024*128)"|bc`

rmfs.xtreemfs -f localhost/performanceTest0 >>$log
rmfs.xtreemfs -f localhost/performanceTest1 >>$log
rmfs.xtreemfs -f localhost/performanceTest2 >>$log

mkfs.xtreemfs localhost/performanceTest0 >>$log
mkfs.xtreemfs localhost/performanceTest1 >>$log
mkfs.xtreemfs localhost/performanceTest2 >>$log

mount.xtreemfs localhost/performanceTest0 ~/xtreemfs0 >>$log
mount.xtreemfs localhost/performanceTest1 ~/xtreemfs1 >>$log
mount.xtreemfs localhost/performanceTest2 ~/xtreemfs2 >>$log

dd if=/dev/zero of=xtreemfs0/test bs=128K count=$count 2> res1.txt &
dd if=/dev/zero of=xtreemfs1/test bs=128K count=$count 2> res2.txt &
dd if=/dev/zero of=xtreemfs2/test bs=128K count=$count 2> res3.txt

sleep 2

echo "WriteBench" >> res.txt
cat res1.txt >> res.txt
echo "\n" >> res.txt
cat res2.txt >> res.txt
echo "\n" >> res.txt
cat res3.txt >> res.txt
echo "\n" >> res.txt

dd if=xtreemfs0/test of=/dev/null bs=128K count=$count 2> res1.txt &
dd if=xtreemfs1/test of=/dev/null bs=128K count=$count 2> res2.txt &
dd if=xtreemfs2/test of=/dev/null bs=128K count=$count 2> res3.txt

sleep 2

echo "ReadBench" >> res.txt
cat res1.txt >> res.txt
echo "\n" >> res.txt
cat res2.txt >> res.txt
echo "\n" >> res.txt
cat res3.txt >> res.txt

rm res1.txt res2.txt res3.txt


rm ~/xtreemfs0/test
rm ~/xtreemfs1/test
rm ~/xtreemfs2/test

umount.xtreemfs ~/xtreemfs0 >>$log
umount.xtreemfs ~/xtreemfs1 >>$log
umount.xtreemfs ~/xtreemfs2 >>$log

rmfs.xtreemfs -f localhost/performanceTest0 >>$log
rmfs.xtreemfs -f localhost/performanceTest1 >>$log
rmfs.xtreemfs -f localhost/performanceTest2 >>$log

cat res.txt
rm res.txt
