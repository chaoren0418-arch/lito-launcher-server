from pathlib import Path
import subprocess,hashlib
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');asm=Path.home()/'.m2/repository/org/ow2/asm'
acp=';'.join(str(asm/f'{a}/9.7/{a}-9.7.jar') for a in ['asm','asm-tree']);cp=str(B/'base.jar')+';'+str(S/'lib/*')
def run(*args):subprocess.run(list(map(str,args)),check=True)
sources=[S/'src/main/java/local/lito381/server'/f'{n}.java' for n in ['SettlementJournal','SettlementSql','DurableBattleSettlement','TeamDisconnectSettlement','DisconnectedBattleResult']]+[S/'tools/shared-database/SharedDatabase.java']
run(J/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',cp,'-d',B,*sources)
run(J/'javac.exe','-encoding','UTF-8','-cp',acp,'-d',B,R/'Patch.java')
run(J/'java.exe','-cp',str(B)+';'+acp+';'+cp,'Patch',B/'base.jar',B,B/'candidate.jar')
print(hashlib.sha256((B/'candidate.jar').read_bytes()).hexdigest())
