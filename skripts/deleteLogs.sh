#!/bin/bash

cd ..
cd serverAndAi	
logs=($(ls | grep "log_game"))
for log in "${logs[@]}"
do
	rm $log
done

