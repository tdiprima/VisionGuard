# https://platform.openai.com/docs/guides/vision#uploading-base64-encoded-images
import base64
import os

from openai import OpenAI

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))


# Function to encode the image
def encode_image(image_path):
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode('utf-8')


# Path to your image
image_path = "example.png"

# Getting the base64 string
base64_image = encode_image(image_path)

response = client.chat.completions.create(model="gpt-4o-mini",
                                          messages=[
                                              {
                                                  "role": "system",
                                                  "content": "You are a JSON-only output generator. Do not provide explanations, preambles, warnings, or additional text beyond JSON. Always return a JSON object in this exact format: {'regions': [{'x': <int>, 'y': <int>, 'width': <int>, 'height': <int>, 'text': '<string>'}, ...]}. Any deviation from this format is an error. Respond with {'regions': []} if no text is detected."
                                              },
                                              {
                                                  "role": "user",
                                                  "content": [
                                                      {
                                                          "type": "text",
                                                          "text": "Analyze the provided image and strictly return the bounding boxes and text regions in JSON format. Do not include anything outside the JSON object.",
                                                      },
                                                      {
                                                          "type": "image_url",
                                                          "image_url": {
                                                              "url": f"data:image/jpeg;base64,{base64_image}"
                                                          },
                                                      },
                                                  ],
                                              }
                                          ])

print(response.choices[0])
