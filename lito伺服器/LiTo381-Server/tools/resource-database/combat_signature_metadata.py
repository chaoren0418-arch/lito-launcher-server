"""Extract parameter semantics only; never compile the broad catalog source."""
import bisect,re

CALCULATIONS={'probability','probabilityForTarget','triggerProbability','successChancePercent','freezeProbability'}
def generate(source,output):
    text=source.read_text(encoding='utf-8-sig')
    text=re.sub(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"',lambda m:' '*len(m.group()),text,flags=re.S)
    declarations=list(re.finditer(r'\b(?:record|class|interface)\s+(\w+)',text))
    starts=[m.start() for m in declarations]
    primitive={'int':'I','long':'J','double':'D','boolean':'Z','float':'F','byte':'B','short':'S','char':'C'}
    result={}
    for method in re.finditer(r'\bint\s+(\w+)\s*\(([^{};]*?)\)\s*(?:\{|;)',text,re.S):
        if method.group(1) not in CALCULATIONS:continue
        owner=declarations[bisect.bisect_right(starts,method.start())-1].group(1)
        params=[];types=[]
        for parameter in method.group(2).split(','):
            words=parameter.strip().split()
            if not words:continue
            typ,name=words[-2:]
            descriptor=primitive.get(typ)
            if descriptor is None:
                descriptor='Ljava/util/function/IntUnaryOperator;' if typ=='IntUnaryOperator' else 'Llocal/lito381/server/'+typ+';'
            types.append(descriptor);params.append(name)
        key=(owner,method.group(1),'('+''.join(types)+')I')
        if key in result:raise ValueError('Ambiguous source calculation signature: '+str(key))
        result[key]=','.join(params)
    output.write_text('\n'.join('\t'.join((*key,names)) for key,names in sorted(result.items()))+'\n',encoding='utf-8')
    return len(result)
