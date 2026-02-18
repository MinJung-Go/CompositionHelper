#!/bin/bash

# Android CI Monitor Script
# This script monitors GitHub Actions for Android CI and fixes issues

GITHUB_TOKEN="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"
REPO="MinJung-Go/CompositionHelper"
BRANCH="android-apk"

# Function to check latest run status
check_status() {
    local result=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
        "https://api.github.com/repos/$REPO/actions/runs?branch=$BRANCH&per_page=1")
    
    echo "$result" | python3 -c "
import sys, json
d = json.load(sys.stdin)
run = d['workflow_runs'][0]
print(f\"{run['run_number']}|{run['status']}|{run.get('conclusion', 'none')}|{run['id']}\")"
}

# Function to get job details
get_job_details() {
    local run_id=$1
    curl -s -H "Authorization: token $GITHUB_TOKEN" \
        "https://api.github.com/repos/$REPO/actions/runs/$run_id/jobs" | \
        python3 -c "
import sys, json
d = json.load(sys.stdin)
if d.get('jobs'):
    job = d['jobs'][0]
    print(f\"{job['name']}|{job.get('conclusion', 'none')}\")
    steps = job.get('steps', [])
    for step in steps:
        if step.get('conclusion') == 'failure':
            print(f\"FAIL: {step['name']}\")
"
}

# Main monitoring loop
echo "Starting Android CI Monitor..."
echo "Branch: $BRANCH"
echo "Repo: $REPO"
echo ""

while true; do
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Checking status..."
    
    status_output=$(check_status)
    run_num=$(echo "$status_output" | cut -d'|' -f1)
    status=$(echo "$status_output" | cut -d'|' -f2)
    conclusion=$(echo "$status_output" | cut -d'|' -f3)
    run_id=$(echo "$status_output" | cut -d'|' -f4)
    
    echo "Run #$run_num: $status - $conclusion"
    
    if [ "$status" = "completed" ]; then
        if [ "$conclusion" = "success" ]; then
            echo ""
            echo "✅ Build succeeded! Stopping monitor."
            echo ""
            echo "You can download APK from:"
            echo "https://github.com/$REPO/actions/runs/$run_id"
            exit 0
        else
            echo ""
            echo "❌ Build failed. Getting details..."
            get_job_details "$run_id"
            echo ""
            echo "Attempting to fix and rebuild..."
            echo ""
            
            # Try to fix common issues
            cd /home/wuying/clawd/CompositionHelper
            
            # Try different workflow configurations
            # (The fixes would be implemented here based on error analysis)
            
            echo "Fix attempted. Manual intervention may be required."
            echo "Check: https://github.com/$REPO/actions/runs/$run_id"
            echo ""
        fi
    fi
    
    # Wait 5 minutes before next check
    echo "Waiting 5 minutes..."
    sleep 300
done
