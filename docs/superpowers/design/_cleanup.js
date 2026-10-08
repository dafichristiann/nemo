const fs = require('fs');
const path = require('path');
const p = 'D:/Perkuliahan/project/reminder/docs/superpowers/design/build_pen.js';
let s = fs.readFileSync(p, 'utf8');

// 1. Replace every non-ASCII run with a clean separator
s = s.replace(/[^\x00-\x7F]+/g, ' - ');

// 2. Tidy the string content artifacts
s = s.replace(/'\s*-\s*/g, "'");            // leading separator after quote
s = s.replace(/\s*-\s*'/g, "'");            // trailing separator before quote
s = s.replace(/  +/g, ' ');
s = s.replace(/- -+/g, '-');
s = s.replace(/' ?' ?Start Focus/g, "'Start Focus");
s = s.replace(/Good morning\s*-?\s*/g, 'Good morning');

fs.writeFileSync(p, s, 'utf8');

const remaining = s.split('\n').filter(l => /[^\x00-\x7F]/.test(l));
console.log('remaining non-ascii lines:', remaining.length);
remaining.slice(0, 10).forEach(l => console.log('  ' + l.trim()));
