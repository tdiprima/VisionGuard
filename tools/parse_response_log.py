import json


def extract_responses(file_path):
    m_responses = []
    m_done_found = False
    with open(file_path, 'r') as file:
        raw_data = file.read()

    # Split based on }{ since objects are not properly concatenated
    fragments = raw_data.split('}{')

    # Add braces to make each fragment valid JSON
    fragments[0] += '}'
    fragments[-1] = '{' + fragments[-1]
    for i in range(1, len(fragments) - 1):
        fragments[i] = '{' + fragments[i] + '}'

    for fragment in fragments:
        try:
            obj = json.loads(fragment)
            if "response" in obj:
                m_responses.append(obj["response"])
            if obj.get("done") is True:
                m_done_found = True
                break
        except json.JSONDecodeError as e:
            print(f"Invalid JSON object: {fragment} | Error: {e}")

    return m_responses, m_done_found


# Parse the file and extract responses
responses, done_found = extract_responses('info.txt')

# Output the responses as one line
output = ''.join(responses)
print(output)

if done_found:
    print("\n'Done' flag found. Stopping extraction.")
else:
    print("\n'Done' flag not found.")
