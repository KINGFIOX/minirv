{
  description = "A flake to provide an environment for NVBoard";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let pkgs = import nixpkgs { inherit system; };
      in {
        devShell = pkgs.mkShell {
          buildInputs = with pkgs; [ sbt verilator circt ];
          shellHook = ''
            export SHELL=$(which zsh)
            exec $SHELL
          '';
        };
      });
}

