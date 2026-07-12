const fs = require('fs');
const path = require('path');

const dir = path.join(__dirname, 'frontend/src/components/ui');
const files = fs.readdirSync(dir);

files.forEach(file => {
    if (file.endsWith('.module.css')) {
        const filePath = path.join(dir, file);
        const content = fs.readFileSync(filePath, 'utf8');
        if (!content.includes('@reference')) {
            fs.writeFileSync(filePath, '@reference "../../app/globals.css";\n\n' + content);
        }
    }
});

console.log("Done");
