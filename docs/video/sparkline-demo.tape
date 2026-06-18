Source shared_.tape

# Setup
Hide
Type jbang sparkline-demo
Enter
Sleep 2
Show

# Recording — compact mode with height changes
Sleep 2s
Type "+"
Sleep 500ms
Type "+"
Sleep 500ms
Type "+"
Sleep 2s
Type "-"
Sleep 500ms
Type "-"
Sleep 500ms
Type "-"
Sleep 500ms
Type "-"
Sleep 2s

# Toggle axes
Type y
Sleep 1s
Type x
Sleep 2s

# Switch to expanded layout
Type@0ms "	"
Sleep 3s

# Back to compact
Type@0ms "	"
Sleep 2s
Screenshot screenshots/sparkline-demo.svg
