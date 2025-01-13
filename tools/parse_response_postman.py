import json


def extract_responses_from_lines(file_path):
    m_responses = []
    m_done_found = False

    with open(file_path, 'r') as file:
        for line in file:
            try:
                obj = json.loads(line.strip())  # Parse each line as a JSON object
                if "response" in obj:
                    m_responses.append(obj["response"])
                if obj.get("done") is True:
                    m_done_found = True
                    break
            except json.JSONDecodeError as e:
                print(f"Invalid JSON object: {line.strip()} | Error: {e}")

    return m_responses, m_done_found


# Parse the file and extract responses
responses, done_found = extract_responses_from_lines('info.txt')

# Output the responses as one line
output = ''.join(responses)
print(output)

if done_found:
    print("\n'Done' flag found. Stopping extraction.")
else:
    print("\n'Done' flag not found.")
