import base64
import re
import os

svg_path = r'C:\Users\Dilsh\AndroidStudioProjects\Qbase\Q base(1).svg'
output_path = r'C:\Users\Dilsh\AndroidStudioProjects\Qbase\temp_icon.png'

with open(svg_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Find the base64 data
match = re.search(r'xlink:href="data:image/png;base64,([^"]+)"', content)
if match:
    base64_data = match.group(1)
    with open(output_path, 'wb') as f:
        f.write(base64.b64decode(base64_data))
    print(f"Successfully extracted PNG to {output_path}")
else:
    print("Base64 PNG data not found in SVG")
