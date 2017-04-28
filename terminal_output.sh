#!/bin/bash

# My terminal config for terminal output
# Substitute tmux-next for tmux for version 1.8

echo "Starting terminal config"
tmux-next new-session -s Thesis -d
tmux-next split-window -h -t Thesis
tmux-next select-pane -t 0 
tmux-next resize-pane -t 1 -R 15 
tmux-next split-window -v -t Thesis
tmux-next select-pane -t 0 
tmux-next resize-pane -t 1 -U 12 
tmux-next select-pane -t 1 
tmux-next split-window -h -t Thesis
tmux-next split-window -v -t Thesis
tmux-next select-pane -t 0
tmux-next attach -t Thesis
echo "Finished terminal config"

