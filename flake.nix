{
  description = "A flake to provide an environment for fpga";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        libPath = pkgs.lib.makeLibraryPath [ ]; # 外部库用在 nix 环境中
        verilatorIncludePath = "${pkgs.verilator}/share/verilator/include";
      in {
        devShell = pkgs.mkShell {
          buildInputs = with pkgs; [
            # scala
            sbt
            temurin-bin-17
            metals
            mill
            metals
            bloop
            # c++
            # rocmPackages.llvm.clang-tools-extra
            verilator
            bear
            # utils
            qemu
            (with pkgsCross.riscv32-embedded; [ buildPackages.gcc ])
            yosys
            verible
          ];

          shellHook = ''
            export C_INCLUDE_PATH=${verilatorIncludePath}:$C_INCLUDE_PATH
            export CPLUS_INCLUDE_PATH=${verilatorIncludePath}:$CPLUS_INCLUDE_PATH
            export MAKEFLAGS="-j$(nproc)"
          '';
        };
      });
}

