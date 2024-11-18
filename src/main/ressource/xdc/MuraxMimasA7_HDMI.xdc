set_property CFGBVS VCCO [current_design]
set_property CONFIG_VOLTAGE 3.3 [current_design]

set_property -dict { PACKAGE_PIN H4    IOSTANDARD LVCMOS33 } [get_ports clk]
set_property -dict { PACKAGE_PIN M2    IOSTANDARD LVCMOS33 } [get_ports reset]

# LEDS
set_property -dict { PACKAGE_PIN K17   IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_led]

# HDMI out
#set_property -dict { PACKAGE_PIN "E2"    IOSTANDARD LVCMOS33 }  [get_ports io_hdmi_tx_cec  ];
set_property -dict { PACKAGE_PIN K3    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_clk_n]
set_property -dict { PACKAGE_PIN L3    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_clk_p]
#set_property -dict { PACKAGE_PIN "B2"    IOSTANDARD LVCMOS33 }  [get_ports io_hdmi_tx_hpd  ]; 
#set_property -dict { PACKAGE_PIN "D2"    IOSTANDARD LVCMOS33 }  [get_ports io_hdmi_tx_rscl ]; 
#set_property -dict { PACKAGE_PIN "C2"    IOSTANDARD LVCMOS33 }  [get_ports io_hdmi_tx_rsda ]; 
set_property -dict { PACKAGE_PIN A1    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tmds_n[0]]
set_property -dict { PACKAGE_PIN B1    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tmds_p[0]]
set_property -dict { PACKAGE_PIN D1    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tmds_n[1]]
set_property -dict { PACKAGE_PIN E1    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tmds_p[1]]
set_property -dict { PACKAGE_PIN F1    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tmds_n[2]]
set_property -dict { PACKAGE_PIN G1    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tmds_p[2]]


# unused ports
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_jtag_tms]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_jtag_tdi]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_jtag_tdo]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_jtag_tck]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_uart_txd]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_uart_rxd]

#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_read[0]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_read[1]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_read[2]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_read[3]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_read[4]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_read[5]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_read[6]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_read[7]]

#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_writeEnable[0]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_writeEnable[1]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_writeEnable[2]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_writeEnable[3]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_writeEnable[4]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_writeEnable[5]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_writeEnable[6]]
#set_property -dict { IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_gpioA_writeEnable[7]]
