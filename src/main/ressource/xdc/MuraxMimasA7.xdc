set_property CFGBVS VCCO [current_design]
set_property CONFIG_VOLTAGE 3.3 [current_design]

set_property -dict { PACKAGE_PIN H4    IOSTANDARD LVCMOS33 } [get_ports io_mainClk]
set_property -dict { PACKAGE_PIN M2    IOSTANDARD LVCMOS33 } [get_ports io_asyncReset]

# LEDS
set_property -dict { PACKAGE_PIN K17   IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_leds[0]]
set_property -dict { PACKAGE_PIN J17   IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_leds[1]]
set_property -dict { PACKAGE_PIN L14   IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_leds[2]]
set_property -dict { PACKAGE_PIN L15   IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_leds[3]]
set_property -dict { PACKAGE_PIN L16   IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_leds[4]]
set_property -dict { PACKAGE_PIN K16   IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_leds[5]]
set_property -dict { PACKAGE_PIN M15   IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_leds[6]]
set_property -dict { PACKAGE_PIN M16   IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_leds[7]]

set_property -dict { PACKAGE_PIN N3    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenEnable[0]]
set_property -dict { PACKAGE_PIN R1    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenEnable[1]]
set_property -dict { PACKAGE_PIN P1    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenEnable[2]]
set_property -dict { PACKAGE_PIN L4    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenEnable[3]]
set_property -dict { PACKAGE_PIN P4    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenData[0]]
set_property -dict { PACKAGE_PIN N4    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenData[1]]
set_property -dict { PACKAGE_PIN M3    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenData[2]]
set_property -dict { PACKAGE_PIN M5    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenData[3]]
set_property -dict { PACKAGE_PIN L5    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenData[4]]
set_property -dict { PACKAGE_PIN L6    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenData[5]]
set_property -dict { PACKAGE_PIN M6    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenData[6]]
set_property -dict { PACKAGE_PIN P5    IOSTANDARD LVCMOS33    SLEW FAST} [get_ports io_sevenData[7]]

# JTAG
set_property -dict { PACKAGE_PIN Y22   IOSTANDARD LVCMOS33       SLEW FAST} [get_ports io_jtag_tck]
set_property -dict { PACKAGE_PIN Y21   IOSTANDARD LVCMOS33       SLEW FAST} [get_ports io_jtag_tdi]
set_property -dict { PACKAGE_PIN AB22  IOSTANDARD LVCMOS33       SLEW FAST} [get_ports io_jtag_tdo]
set_property -dict { PACKAGE_PIN AA21   IOSTANDARD LVCMOS33       SLEW FAST} [get_ports io_jtag_tms]

# HDMI out
#set_property -dict { PACKAGE_PIN "E2"    IOSTANDARD LVCMOS33 }  [get_ports io_hdmi_tx_cec  ];
set_property -dict { PACKAGE_PIN "K3"    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_clk_n]
set_property -dict { PACKAGE_PIN "L3"    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_clk_p]
#set_property -dict { PACKAGE_PIN "B2"    IOSTANDARD LVCMOS33 }  [get_ports io_hdmi_tx_hpd  ]; 
#set_property -dict { PACKAGE_PIN "D2"    IOSTANDARD LVCMOS33 }  [get_ports io_hdmi_tx_rscl ]; 
#set_property -dict { PACKAGE_PIN "C2"    IOSTANDARD LVCMOS33 }  [get_ports io_hdmi_tx_rsda ]; 
set_property -dict { PACKAGE_PIN "A1"    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tdms_n[0]]
set_property -dict { PACKAGE_PIN "B1"    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tdms_p[0]]
set_property -dict { PACKAGE_PIN "D1"    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tdms_n[1]]
set_property -dict { PACKAGE_PIN "E1"    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tdms_p[1]]
set_property -dict { PACKAGE_PIN "F1"    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tdms_n[2]]
set_property -dict { PACKAGE_PIN "G1"    IOSTANDARD TMDS_33  }  [get_ports io_hdmi_tdms_p[2]]


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
