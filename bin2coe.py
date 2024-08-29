import sys

def bin2coe(bin_file, coe_file):
    try:
        with open(bin_file, 'rb') as f:
            binary_data = f.read()

        words = []
        for i in range(0, len(binary_data), 4):
            word = binary_data[i:i+4]
            # Ensure the word is 4 bytes long by padding with zeros if necessary
            if len(word) < 4:
                word = word.ljust(4, b'\x00')
            # Convert the word to little-endian format
            little_endian_word = word[::-1]
            # Convert to hex string
            words.append(little_endian_word.hex())

        with open(coe_file, 'w') as f:
            f.write("memory_initialization_radix=16;\n")
            f.write("memory_initialization_vector=\n")
            f.write("\n".join(words))

        print(f"Conversion complete: {coe_file} generated successfully.")
    except FileNotFoundError:
        print(f"Error: The file {bin_file} was not found.")
    except Exception as e:
        print(f"An error occurred: {str(e)}")

if __name__ == "__main__":
    bin2coe("random.bin", "random.coe")