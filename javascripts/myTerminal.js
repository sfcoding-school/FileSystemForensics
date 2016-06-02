////////////////////////////////////////////////////////////////////////////////
///////////////////////////// UTILITY /////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

function what_to_do(command, term){ manager(command, term);}

Array.prototype.clean = function(deleteValue) {
  for (var i = 0; i < this.length; i++) {
    if (this[i] == deleteValue) {         
      this.splice(i, 1);
      i--;
    }
  }
  return this;
};

function goToCurrentFolder_recursive(path, json_temp){
    for (var i = 0; i < json_temp.length; i++) {
        if (json_temp[i]["nome"] == path[0]) {
            return goToCurrentFolder_recursive(path.slice(1, path.length), json_temp[i]["sub"]);
        }
    }
    console.log("path" + path.length)
    if (path.length == 0) return json_temp;
    return false;
}

function goToCurrentFolder(){
    console.log(currentFolder)
    if (currentFolder == "/" || currentFolder == "~") {
        return global_json["FileSystem"];
    } else {
        var temp = currentFolder.split("/");
        console.log(temp)

        return goToCurrentFolder_recursive(temp.slice(1, temp.length), global_json["FileSystem"])

    }
}

function checkExistingPath(path){
    c_t = currentFolder + "/" + path;
    if(goToCurrentFolder_recursive(c_t.split("/").slice(1, c_t.length), global_json["FileSystem"]) == false)
        return false;
    else
        return true;
}

////////////////////////////////////////////////////////////////////////////////
///////////////////////////// COMMANDS /////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

function ls(commands, term){
    if (commands.length > 1){
        term.error("I don't understand this. Try ls --help");
        return;
    }
    if (commands[0] == "--help" ) {
        term.echo("I'll output some helps about ls command");
        return;
    } else if(commands.length==0){
        term.echo("I'll do something in the current folder");

        if (global_json != undefined) {
            console.log(global_json); // this will show the info it in firebug console
            term.echo("Sha: " + global_json["sha1"]);
            jsonC = goToCurrentFolder();
            console.log(jsonC.length);
            for (var i = 0; i < jsonC.length; i++) {
                if (jsonC[i]["isDirectory"] == true)
                    term.echo('[[b;#0080FF;#000]' + jsonC[i]["nome"] + ']')
                else
                    term.echo(jsonC[i]["nome"] + "\t" + jsonC[i]["lastModDate"]);
            }
        }

        return;
    } else {
        term.echo("hai scritto ls per path: " + commands[0]);
    }
}

function cd(commands, term){
    if (commands.length==0 || commands.length > 1){
        term.error("I don't understand this. Try cd --help");
        return;
    }
    if (commands[0] == "--help" ) {
        term.echo("I'll output some helps about cd command");
        return;
    } else {
        term.echo("hai scritto cd per path: " + commands[0]);

        if (commands[0].indexOf("..") > -1) {
            console.log("volevo andare indietro. DA GESTIRE");

            if (commands[0] == ".." || commands[0] == "../") {
                if (currentFolder != "/" || currentFolder != "~") {

                    var temp = currentFolder.split("/");
                    temp = temp.slice(0, temp.length-1);
                    currentFolder = temp.join("/");
                    console.log(currentFolder)
                    term.push(what_to_do, {prompt: 'user:' + currentFolder + '> '});
                }
            }

            return;
            // se è "cd .." o "../" è facile perchè posso considerare 
            // che sono già su un path esatto e cancellare solo l'ultima parte
        }

        if (checkExistingPath(commands[0])) {
            //al momento non gestico caso path assoluto
            currentFolder = currentFolder + "/" + commands[0];
            term.push(what_to_do, {prompt: 'user:' + currentFolder + '> '});
        }

    }
}

function help(term){
    term.echo("I'll output some command list in the future")
}

function manager(command, term){
    var commands = command.split(" ").clean("");

    if (commands[0] == "help") help(term);
    else if (commands[0] == "ls") ls(commands.slice(1, commands.length), term);
    else if (commands[0] == "cd") cd(commands.slice(1, commands.length), term);
    else term.echo("Your command doesn't exists. Try to type: help");
}