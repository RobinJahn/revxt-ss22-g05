#!/bin/bash

#in current folder
logs=($(ls | grep "log_game"))
for log in "${logs[@]}"
do
	rm $log
done

#in server folder
cd ..
cd serverAndAi	
logs=($(ls | grep "log_game"))
for log in "${logs[@]}"
do
	rm $log
done

