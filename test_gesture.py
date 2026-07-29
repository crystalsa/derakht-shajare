import urllib.request

url = "https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/TransformGestureDetector.kt"
response = urllib.request.urlopen(url)
data = response.read().decode('utf-8')

with open("TransformGestureDetector.kt", "w") as f:
    f.write(data)
