from os import environ
from pyftdi.ftdi import Ftdi
from pyftdi.jtag import JtagEngine, JtagTool
from pyftdi.bits import BitSequence
from pyftdi.usbtools import UsbTools

import sys


# protocol://vendor:product[:serial|:index|:bus:addr]/interface
url = 'ftdi://0x2a19:0x1009/0'

jtag = JtagEngine(frequency=100e3)
jtag.controller.ftdi.add_custom_product(0x2a19, 0x1009, 'numato')
jtag.configure(url)

jtag.reset()    # Test Logic Reset (5 times of TMS=1)

tool = JtagTool(jtag)


# go into RESET state
jtag.reset()

jtag.go_idle()
idcode = tool.idcode()
print(f'IDCODE: 0x{idcode:08x}')

jtag.go_idle()
idcode = tool.idcode()
print(f'IDCODE: 0x{idcode:08x}')

jtag.go_idle()
idcode = int(jtag.read_dr(32))
print(f'IDCODE: 0x{idcode:08x}')

#jtag.write_tms(BitSequence('11111'))

sys.exit()

# Move to Run-Test/Idle if required
jtag.go_idle()

# To Shift-IR, Exit1-IR and finally move to Update-IR
jtag.write_ir(BitSequence('00000101', msb=True, length=8))

jtag.write_dr(BitSequence(0, msb=False, length=8))
# The following looks required to reflect DR change!?
jtag.go_idle()

sys.exit(0)