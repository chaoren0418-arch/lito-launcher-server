@echo off
for %%r in (tw-bin-engine lito-launcher-server lito-modern) do (
  gh label create local-task -R chaoren0418-arch/%%r -c 0E8A16 -d "需要本機執行的任務" --force
  gh label create running    -R chaoren0418-arch/%%r -c FBCA04 --force
  gh label create done       -R chaoren0418-arch/%%r -c 0075CA --force
  gh label create failed     -R chaoren0418-arch/%%r -c D93F0B --force
)
pause
