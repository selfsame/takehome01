const http = require('http')
const fs = require('fs')
const path = require('path')

const contentTypes = {
    '.html': 'text/html',
    '.js': 'text/javascript',
    '.css': 'text/css',
    '.png': 'image/png',
    '.jpg': 'iamge/jpg',
    '.map': 'application/json'}

http.createServer(function (req, res) {
    var url = path.normalize(req.url)
    if (url ==='/api/search'){
        res.writeHead(200, {'Content-Type': 'application/json'})
        res.write('{"foo":"bar"}') 
        res.end()
    } else {
        url = "./resources/public"+url
        // handle directory resolving to .index
        try {
            if (fs.lstatSync(url).isDirectory() ) {
                url += "/index.html"
            }
        } catch (error) {}
        
        fs.readFile(url, (err, data) => {
            if (err) {
                console.log(err)
                res.writeHead(404, {'Content-Type': 'text/html'})
                res.write('<h1>404 Not Found<h1>')
                res.end()
            } else {
                res.setHeader("Content-Type", contentTypes[path.extname(url)])
                res.writeHead(200)
                res.end(data)
            }
        })
    }
}).listen(8000, function(){
    console.log("server start at port 8000")
})