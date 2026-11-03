"""Frame events retained independently of either client's in-memory layout."""
from dataclasses import dataclass
import re

@dataclass(frozen=True)
class Frame:
    image:int
    index:int
    ticks:int
    sound_mode:str
    sounds:tuple[int,...]
    event:str
    event_value:int|None
    attack:bool
    restart:bool

class Cursor:
    def __init__(self,text):self.text=text;self.at=0
    def peek(self):return self.text[self.at:self.at+1]
    def number(self):
        # The native lexer ignores punctuation/labels, preserving '-' across
        # spaces. It leaves the cursor on the first non-digit after a number.
        negative=False
        while self.at<len(self.text) and not '0'<=self.peek()<='9':
            ch=self.peek()
            if ch=='-':negative=True
            elif ch!=' ':negative=False
            self.at+=1
        start=self.at
        while self.peek() and '0'<=self.peek()<='9':self.at+=1
        if start==self.at:raise ValueError('missing native numeric token')
        value=int(self.text[start:self.at]);return -value if negative else value

def parse_action(body):
    c=Cursor(body);direction=c.number();count=c.number()
    if not 0<=direction<=1 or not 0<=count<=127:raise ValueError('native action header out of range')
    frames=[]
    for _ in range(count):
        image=c.number();index=c.number();ticks=c.number()
        mode=c.peek() if c.peek() in ('<','[','^') else ''
        sounds=[]
        if mode:
            while c.peek()==mode:sounds.append(c.number())
        event=c.peek() if c.peek() in (']','{','}') else ''
        value=c.number() if event else None
        attack=c.peek()=='!'
        if attack:c.at+=1
        restart=c.peek()=='>'
        if restart:c.at+=1
        frames.append(Frame(image,index,ticks,mode,tuple(sounds),event,value,attack,restart))
    if re.sub(r'[\s()ij]','',c.text[c.at:]):
        raise ValueError('unconsumed action data: '+repr(c.text[c.at:c.at+100]))
    return direction,frames
