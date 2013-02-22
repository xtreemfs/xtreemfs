#/bin/sh

dd if=/dev/zero of=test1 bs=128K count=32768 > res1.txt &
dd if=/dev/zero of=test2 bs=128K count=32768 > res2.txt &
dd if=/dev/zero of=test3 bs=128K count=32768 > res3.txt

cat res1.txt >> res.txt
cat res2.txt >> res.txt
cat res3.txt >> res.txt

rm res1.txt res2.txt res3.txt
