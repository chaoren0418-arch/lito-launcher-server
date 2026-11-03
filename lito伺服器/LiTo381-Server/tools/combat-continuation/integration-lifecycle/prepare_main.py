from pathlib import Path
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build'
for original,target in [('probe-native-pair-material-Main.py','test_pair_main.py'),('replay-native-pair-material-skill73-Main.py','test_skill73_main.py')]:
 t=(B/original).read_text('utf-8')
 t=t.replace('B=Path(__file__).resolve().parent',"B=S/'tools/resource-database/build'")
 old="D=B/'de-native-pair-material-development'";assert t.count(old)==1;t=t.replace(old,"D=Path(__file__).resolve().parent/'build'")
 t=t.replace("build=read(D/'build-current.json')","build=read(D/'pair-build-current.json')")
 marker="verifier=inputs/'VerifyNativeMagicalWeaponMainMemoryFixture.java'";assert t.count(marker)==1
 if target=='test_pair_main.py':
  patch="fixture=inputs/'NativePairMaterialMainProbe.java';text=fixture.read_text('utf-8');assert text.count('NativePairCastAction.class')==1;fixture.write_text(text.replace('NativePairCastAction.class','NativePairedCastSafety.class'),encoding='utf-8')\n"
 else:
  patch="fixture=inputs/'NativeMagicalWeaponMainProbe.java';text=fixture.read_text('utf-8');assert text.count('NativeMagicalWeaponCastAction.class')==1;fixture.write_text(text.replace('NativeMagicalWeaponCastAction.class','NativeMagicalWeaponAction.class'),encoding='utf-8')\n"
 t=t.replace(marker,patch+marker)
 (R/target).write_text(t,encoding='utf-8')
print('prepared exact Main10+8 with current integrated origin-type names and isolated proof paths; memory/port fixture inverse and native assertions retained')
