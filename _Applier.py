import tqdm
#import PatchProcess
import argparse
import os
def getCode(pat):
    #uncheck for len of code
    return pat[pat.find('$')+1:]
def preprocess(pat):
    #patch for simplified grammar
    if pat.startswith('insert-before$'):
        return pat.replace('insert-before$', 'insert-before:0$', 1)
    elif pat.startswith('insert-after$'):
        return pat.replace('insert-after$', 'insert-after:0$', 1)
    elif pat.startswith('replace$'):
        return pat.replace('replace$', 'replace:0,1$', 1)
    elif pat.startswith('wrap$'):
        return pat.replace('wrap$', 'wrap:0,1$', 1)
    elif pat == 'delete':
        return 'delete:0,1'
    else:
        test_tks = pat.split(':')
        test_tks[1] = pat[pat.find(':')+1:]
        if test_tks[0] not in ['delete', 'replace', 'wrap']:
            return pat
        elif test_tks[0] == 'delete' and ',' not in test_tks[1]:
            return test_tks[0] + ':0,' + test_tks[1]
        elif ',' not in test_tks[1][:test_tks[1].find('$')]:
            return test_tks[0] + ':0,' +test_tks[1]
        else:
            return pat
def tokenize(pat):
    pat = preprocess(pat)
    #uncheck the error grammar
    oper = pat.split(':')[0]
    if oper not in ['delete', 'move-before', 'move-after']:
        code = getCode(pat)
        pos = []
        if len(code) == 0:
            oper = 'skip'
            return (oper)
        _pos = pat[pat.find(':')+1:pat.find('$')].split(',')
        for i in _pos:
            pos.append(int(i))
        #tuple (str:operation, [int:positions..], str:code]
        return (oper, pos, code)
    else:
        pos = []
        _pos = pat.split(':')[1]
        _pos = _pos.split(',')
        for i in _pos:
            pos.append(int(i))
        #tuple (str:operation, [int:positions..])
        return (oper, pos)
def getTargetLine(line, pat):
    return line + tokenize(pat)[1][0]
def toSrcList(src):
    res = []
    for code in src:
        if type(code) == tuple:
            res.append(code[0]+code[1]+code[2])
        else:
            res.append(code)
    return res
def toSrc(src):
    return ''.join(toSrcList(src))
def toFixedLineSrc(src):
    len0 = len(src)
    srclist = toSrcList(src)
    srclist = list(map(lambda x : x.replace('\n', ''), srclist))
    srclist = list(map(lambda x : '{}\n'.format(x), srclist))
    assert len(srclist) == len0
    return ''.join(srclist)
def ins_bf(src, line, bias, code):
    cp = line - 1 + bias[0]
    if type(src[cp]) == tuple:
        src[cp] = (src[cp][0]+' '+code, src[cp][1], src[cp][2])
    else:
        src[cp] = (code, src[cp], '')
    return src
def ins_af(src, line, bias, code):
    cp = line - 1 + bias[0]
    if type(src[cp]) == tuple:
        src[cp] = (src[cp][0], src[cp][1], code+' '+src[cp][2])
    else:
        src[cp] = ('', src[cp], code)
    return src
def ins_ba(src, line, bias, code1, code2):
    return ins_af(ins_bf(src, line, [bias[0]], code1), line, [bias[1]], code2)
def del_(src, line, bias):
    i = line-1+bias[0]
    while i != line+bias[0]+bias[1]-1:
        if type(src[i]) == tuple:
            src[i] = (src[i][0], '', src[i][2])
        else:
            src[i] = ('', '', '')
        i+=1
    return src
def rep(src, line, bias, code):
    i = line-1+bias[0]
    if type(src[i]) == tuple:
        src[i] = (src[i][0], code, src[i][2])
    else:
        src[i] = ('', code, '')
    i+=1
    while i != line+bias[0]+bias[1]-1:
        if type(src[i]) == tuple:
            src[i] = (src[i][0], '', src[i][2])
        else:
            src[i] = ('', '', '')
        i+=1
    return src
def wrap(src, line, bias, code):
    src = ins_bf(src, line, [bias[0]], code)
    src = ins_af(src, line, [bias[0]+bias[1]-1], '}')
    return src
def mv_b(src, line, bias):
    code = ''.join(toSrcList(src[line-1+bias[0]:line-1+bias[0]+bias[1]]))
    i=line - 1 + bias[0]
    while i != line - 1 + bias[0] + bias[1]:
        src[i] = ''
        i += 1
    return ins_bf(src, line, [bias[2]], code)
def mv_a(src, line, bias):
    code = ''.join(toSrcList(src[line-1+bias[0]:line-1+bias[0]+bias[1]]))
    i=line - 1 + bias[0]
    while i != line - 1 + bias[0] + bias[1]:
        src[i] = ''
        i += 1
    return ins_af(src, line, [bias[2]], code)
def applyPatch(src, pat, line):
    assert type(src) == list
    tp = tokenize(pat)
    op = tp[0]
    if op == 'insert-before':
        return ins_bf(src, line, tp[1], tp[2])
    elif op == 'insert-after':
        return ins_af(src, line, tp[1], tp[2])
    elif op == 'insert-before-after':
        code = tp[2]
        code = code.split('<#>')
        return ins_ba(src, line, tp[1], code[0], code[1])
    elif op == 'delete':
        return del_(src, line, tp[1])
    elif op == 'replace':
        return rep(src, line, tp[1], tp[2])
    elif op == 'wrap':
        return wrap(src, line, tp[1], tp[2])
    elif op == 'move-before':
        return mv_b(src, line, tp[1])
    elif op == 'move-after':
        return mv_a(src, line, tp[1])
    else:
        print('{}, un change'.format(op))
        return src
'''def make(src, pat, line, out, ori):
    PP = PatchProcess.PatchProcessor()
    if not os.path.exists(out):
        os.makedirs(out)
    with open(src, 'r') as s:
        src = s.readlines()
        src_str = ''.join(src)
    with open(ori, 'r') as o:
        ori = o.readlines()
        ori_s = ''.join(ori)
    with open(pat, 'r') as p:
        pat = p.readlines()
    ct = 0
    for patch in tqdm.tqdm(pat):
        srcp = src[0:]
        try:
            sb,pb = PP.extractMethodForSrc(ori_s, ''.join(apply_(srcp, patch, line)))
        except Exception:
            continue
        with open(out+'/'+str(ct)+'_sb', 'w') as w:
            w.write(sb)
        with open(out+'/'+str(ct)+'_pb', 'w') as wp:
            wp.write(pb)
        ct += 1
if __name__ == '__main__':
    parser = argparse.ArgumentParser(description = 'pa')
    parser.add_argument('-m', '--mode', required=False, default='ap')
    parser.add_argument('-s', '--src', required=False)
    parser.add_argument('-p', '--pat', required=False)
    parser.add_argument('-l', '--line', required=False, type=int)
    parser.add_argument('-o', '--out', required=False)
    parser.add_argument('-i', '--ori', required=False)
    args = parser.parse_args()
    if args.mode == 'ap':
        make(args.src, args.pat, args.line, args.out, args.ori)'''
