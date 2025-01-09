import requests
import base64

url = "http://localhost:11434/api/generate"
image_path = "example.png"

# Read and encode the image
with open(image_path, "rb") as image_file:
    encoded_string = base64.b64encode(image_file.read()).decode("utf-8")

payload = {
    "model": "llama3.2-vision:latest",
#    "system": "You are a JSON-only generator...",
    "prompt": "Find all text in the image. If you did not receive the image, just say so.",
    "image": encoded_string,
}
headers = {"Content-Type": "application/json"}

response = requests.post(url, json=payload, headers=headers)
print(response.content.decode('utf-8'))
