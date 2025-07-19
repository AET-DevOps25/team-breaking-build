#!/usr/bin/env python3
"""
Test runner script for the GenAI service.

This script provides convenient ways to run tests with different options.
"""

import sys
import subprocess
import argparse
import os
from pathlib import Path


def run_command(cmd, description=""):
    """Run a command and handle errors"""
    print(f"\n{'='*60}")
    if description:
        print(f"Running: {description}")
    print(f"Command: {' '.join(cmd)}")
    print(f"{'='*60}")
    
    try:
        result = subprocess.run(cmd, check=True, capture_output=False)
        print(f"\n✅ {description} completed successfully!")
        return True
    except subprocess.CalledProcessError as e:
        print(f"\n❌ {description} failed with exit code {e.returncode}")
        return False


def main():
    parser = argparse.ArgumentParser(description="Run GenAI service tests")
    parser.add_argument(
        "--type", 
        choices=["unit", "integration", "all"], 
        default="all",
        help="Type of tests to run"
    )
    parser.add_argument(
        "--verbose", "-v", 
        action="store_true",
        help="Verbose output"
    )
    parser.add_argument(
        "--coverage", 
        action="store_true",
        help="Run with coverage report"
    )
    parser.add_argument(
        "--parallel", "-n", 
        type=int,
        help="Run tests in parallel (requires pytest-xdist)"
    )
    parser.add_argument(
        "--slow", 
        action="store_true",
        help="Include slow tests"
    )
    parser.add_argument(
        "--fast", 
        action="store_true",
        help="Skip slow tests"
    )
    parser.add_argument(
        "--pattern", 
        help="Pattern to match test files or test names"
    )
    
    args = parser.parse_args()
    
    # Build pytest command
    cmd = ["python", "-m", "pytest"]
    
    # Add verbosity
    if args.verbose:
        cmd.append("-v")
    
    # Add coverage if requested
    if args.coverage:
        cmd.extend(["--cov=.", "--cov-report=html", "--cov-report=term-missing"])
    
    # Add parallel execution if requested
    if args.parallel:
        cmd.extend(["-n", str(args.parallel)])
    
    # Add test type filters
    if args.type == "unit":
        cmd.append("-m")
        cmd.append("unit")
    elif args.type == "integration":
        cmd.append("-m")
        cmd.append("integration")
    
    # Add slow test filters
    if args.slow:
        cmd.append("--runslow")
    elif args.fast:
        cmd.append("-m")
        cmd.append("not slow")
    
    # Add pattern if specified
    if args.pattern:
        cmd.append("-k")
        cmd.append(args.pattern)
    
    # Add test discovery - run from current directory (test/)
    cmd.extend([".", "-v"])
    
    # Change to the parent directory to run tests properly
    original_dir = os.getcwd()
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    # Run the tests
    success = run_command(cmd, f"Running {args.type} tests")
    
    # Change back to original directory
    os.chdir(original_dir)
    
    if success:
        print("\n🎉 All tests passed!")
        return 0
    else:
        print("\n💥 Some tests failed!")
        return 1


if __name__ == "__main__":
    sys.exit(main()) 