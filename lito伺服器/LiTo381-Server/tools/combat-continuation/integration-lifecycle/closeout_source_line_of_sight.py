"""Extract the three exact readonly Source LOS methods; no game execution."""
import re


def geometry_source(text):
    methods = []
    for name in ['headingTowardAdjacentCell', 'hasLineOfSight', 'isProjectileCardinalBoundaryOpen']:
        pattern = r'(?m)^\t(?:public|private) static (?:boolean|int) ' + name + r'\('
        hits = list(re.finditer(pattern, text))
        if len(hits) != 1:
            raise ValueError('Review Source LOS method: ' + name)
        start = hits[0].start(); body = text.index('{', start); depth = 1; end = body + 1
        while depth:
            char = text[end]
            depth += (char == '{') - (char == '}')
            end += 1
        methods.append(text[start:end].replace('L1Map', 'NativeDeTerrainV1'))
    return ('package local.lito381.server;\n\n'
            '/** Exact Source L1Character LOS bodies, with only the map type relocated. */\n'
            'final class NativeCloseoutSourceLineOfSight {\n' + '\n\n'.join(methods)
            + '\n    private NativeCloseoutSourceLineOfSight() {}\n}\n')
