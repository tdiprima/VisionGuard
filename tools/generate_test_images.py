from PIL import Image, ImageDraw, ImageFont


def create_test_image(m_text, m_font_size, bounding_box_color, save_path):
    width, height = 800, 400
    image = Image.new("RGB", (width, height), color="white")
    draw = ImageDraw.Draw(image)

    # Load a font
    try:
        font = ImageFont.truetype("Arial.ttf", m_font_size)
    except IOError:
        font = ImageFont.load_default()  # Use default if no font available

    # Draw text
    text_bbox = draw.textbbox((0, 0), m_text, font=font)  # Returns (left, top, right, bottom)
    text_width = text_bbox[2] - text_bbox[0]
    text_height = text_bbox[3] - text_bbox[1]
    text_position = ((width - text_width) // 2, (height - text_height) // 2)
    draw.text(text_position, m_text, fill="black", font=font)

    # Calculate bounding box and print it
    bbox = [text_position[0] - 10, text_position[1] - 10,  # Top-left
            text_position[0] + text_width + 10, text_position[1] + text_height + 10  # Bottom-right
            ]
    print(f'Bounding box for "{m_text}": {bbox}')
    print(f'Width: {text_width}, Height: {text_height}')

    # Save the image
    image.save(save_path)
    print(f"Test image saved to {save_path}")


# Generate test cases
test_cases = [
    ("Small Text", 20, "red", "test_small_text.png"),
    ("Medium Text", 40, "green", "test_medium_text.png"),
    ("Large Text", 80, "blue", "test_large_text.png"),
]

for text, font_size, color, path in test_cases:
    create_test_image(text, font_size, color, path)
