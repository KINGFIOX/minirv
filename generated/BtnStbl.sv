// Generated by CIRCT firtool-1.62.0
module BtnStbl (
    input  clock,
    reset,
    io_btn_in,
    output io_btn_out
);

  reg         btn_sync_REG;
  reg         btn_sync;
  reg         btn_stable;
  reg  [13:0] wrap_c_value;
  wire        wrap_wrap_wrap = wrap_c_value == 14'h3A97;
  wire        _GEN = btn_sync == btn_stable;
  wire        wrap = ~_GEN & wrap_wrap_wrap;
  always @(posedge clock) begin
    btn_sync_REG <= io_btn_in;
    btn_sync <= btn_sync_REG;
    if (reset) begin
      btn_stable   <= 1'h0;
      wrap_c_value <= 14'h0;
    end else begin
      if (wrap) btn_stable <= btn_sync;
      if (~_GEN) begin
        if (wrap_wrap_wrap) wrap_c_value <= 14'h0;
        else wrap_c_value <= wrap_c_value + 14'h1;
      end
    end
  end  // always @(posedge)
  assign io_btn_out = wrap;
endmodule

