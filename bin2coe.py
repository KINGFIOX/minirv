def bin2coe(bin_file, coe_file):
    with open(bin_file, "rb") as bin_f, open(coe_file, "w") as coe_f:
        # Write COE header
        coe_f.write("memory_initialization_radix=16;\n")
        coe_f.write("memory_initialization_vector=\n")

        # Read binary data and convert to hex
        byte = bin_f.read(1)
        hex_values = []
        while byte:
            hex_values.append(f"{int.from_bytes(byte, 'little'):02X}")
            byte = bin_f.read(1)

        # Write hex values to COE file
        coe_f.write(",\n".join(hex_values) + ";\n")

if __name__ == "__main__":
    bin2coe("random.bin", "random.coe")