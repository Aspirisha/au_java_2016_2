#!/bin/bash
rm -r .rush/
echo text > file
./rush init
./rush add file
./rush commit -m "initial commit"
echo "some more text" >> file
./rush add file
./rush commit -m "second commit"
./rush branch b2
