#!/bin/bash

cd ..

logs=($(find "$PWD" | grep "log_game_[0-9]*.[0-9]*\.txt"))
for log in "${logs[@]}"
do
	rm $log
done
