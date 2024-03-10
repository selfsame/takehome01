const http = require('http')
const https = require('https')
const urllib = require('url')
const fs = require('fs')
const path = require('path')

// This should be gitignored to keep it out of git but I am including it as a courtesy to the reviewer
const API_KEY = fs.readFileSync('giant_bomb_api_key', {encoding: 'utf-8'})

const contentTypes = {
    '.html': 'text/html',
    '.js': 'text/javascript',
    '.css': 'text/css',
    '.png': 'image/png',
    '.jpg': 'iamge/jpg',
    '.map': 'application/json'}

function fetch(host, path, cb){
    https.get({hostname: host,
               path: path,
               headers: {'User-Agent':'selfsame'}}, (res) => {
        let body = "";
        res.on("data", (chunk) => {
            body += chunk;
        })
        res.on("end", () => {
            cb(body)
        })
    })
}

    
// http://localhost:8000/api/search?filter=name:fantasy&field_list=id,name,image&limit=10

http.createServer(function (req, res) {
    var url = path.normalize(req.url)
    if (url.startsWith('/api/search')){
        // effectively just routing client requests to the giantbomb server with our API key
        // client parameters like search terms are tacked on 'as is'
        params = urllib.parse(url, false).query
        fetch('www.giantbomb.com', 
            `/api/games/?format=json&api_key=${API_KEY}`+'&'+params,
            (body)=> {
                res.writeHead(200, {'Content-Type': 'application/json'})
                res.write(body) 
                res.end()
            })
        
    } else {
        url = "./resources/public"+urllib.parse(url, false).pathname
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