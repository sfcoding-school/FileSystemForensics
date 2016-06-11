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

String.prototype.padding = function(n, c)
{
  var val = this.valueOf();
  if ( Math.abs(n) <= val.length ) {
    return val;
  }
  var m = Math.max((Math.abs(n) - this.length) || 0, 0);
  var pad = Array(m + 1).join(String(c || ' ').charAt(0));
  //      var pad = String(c || ' ').charAt(0).repeat(Math.abs(n) - this.length);
  return (n < 0) ? pad + val : val + pad;
  //      return (n < 0) ? val + pad : pad + val;
};

function goToCurrentFolder_recursive(path, json_temp){
  for (var i = 0; i < json_temp.length; i++) {
    if (json_temp[i]["nome"].replace(/\s/g, '#') == path[0]) {
      return goToCurrentFolder_recursive(path.slice(1, path.length), json_temp[i]["sub"]);
    }
  }
  console.log("path" + path.length);
  if (path.length === 0) return json_temp;
  return false;
}

function goToCurrentFolder(){
  console.log(currentFolder);
  if (currentFolder == "/" || currentFolder == "~") {
    return global_json["FileSystem"];
  } else {
    var temp = currentFolder.replace(/\\ /g, '#').split("/");
    console.log(temp);

    return goToCurrentFolder_recursive(temp.slice(1, temp.length), global_json["FileSystem"]);

  }
}

function checkExistingPath(path){
  c_t = currentFolder + "/" + path;
  if(goToCurrentFolder_recursive(c_t.split("/").slice(1, c_t.length), global_json["FileSystem"]) === false)
  return false;
  else
  return true;
}

function findFolder(){
  var tab_help = [];

  jsonC = goToCurrentFolder();
  for (var i = 0; i < jsonC.length; i++) {
    if (jsonC[i]["isDirectory"] === true){
      tab_help.push(jsonC[i]["nome"]);
    }
  }
  return tab_help;
}

function findFile(){
  var tab_help = [];

  jsonC = goToCurrentFolder();
  for (var i = 0; i < jsonC.length; i++) {
    if (jsonC[i]["isDirectory"] === false){
      tab_help.push(jsonC[i]["nome"]);
    }
  }

  return tab_help;
}

function formatTime(unixTimeStamp) {
  var dateTime = new Date(unixTimeStamp*1000);
  return dateTime.toISOString().replace("T", " "); // Returns "2013-05-31T11:54:44.000Z";
}

////////////////////////////////////////////////////////////////////////////////
///////////////////////////// COMMANDS /////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

function takeAll(term){
  $.ajax({
    async: false,
    type: 'GET',
    url: "http://127.0.0.1:8000/terminal?id=x&cmd=takeAll",
    success: function(data, status){
      console.log("callback takeAll");
      term.echo("The request has been sent");
    }
  });
}

function ls(commands, term){
  console.log(commands.length);
  if (commands.length > 2){
    term.error("I don't understand this. Try ls --help");
    return;
  }
  var long_list_format = false;
  if (commands.length > 0){
    if(commands[0]=="-l") {
      long_list_format = true;
      commands = commands.slice(1, commands.length);
    } else if(commands[1]=="-l" ){
      long_list_format = true;
      commands = commands.slice(0, -1);
    }
  }

  console.log(commands[0] + " " + commands.length);

  if (commands[0] == "--help" ) {
    var output = "Use: ls [OPZIONE]... [FILE]...\n";
    output += "List information about the current directory\n";
    output += " -l\tuse a long listing format\n";
    term.echo(output);
    return;
  } else if(commands.length === 0){
    // term.echo("I'll do something in the current folder");

    if (global_json !== undefined) {
      // console.log(global_json);
      // term.echo("Sha: " + global_json["sha1"]);
      jsonC = goToCurrentFolder();
      var myOut = "";

      for (var i = 0; i < jsonC.length; i++) {

        if (jsonC[i]["isDirectory"] === true){
          myOut += '[[b;#0080FF;#000]' + jsonC[i]["nome"] + ']\n';
        } else {
          myOut += jsonC[i]["nome"].padding(30);
          if (long_list_format) {
            myOut += "\t" + (jsonC[i]["Byte"].toString() + "B").padding(8) + "\t(" + jsonC[i]["lastModDate"] + ")\r\n";
          } else {
            myOut += "\r\n";
          }

        }
      }
      term.echo(myOut);
    }

    return;
  } else {
    term.echo("hai scritto ls per path: " + commands[0]);
  }

}

function cd(commands, term){
  if (commands.length === 0){
    term.error("I don't understand this. Try cd --help");
    return;
  }

  if (commands.join(" ").replace(/\\ /g, '#').split(" ").length > 1) {
    term.error("Too many args for cd command");
    return;
  } else {
    commands = commands.join(" ").replace(/\\ /g, '#').split(" ");
  }

  if (commands[0] == "--help" ) {
    var output = "Use: cd [DIR]\n";
    output += "Change the current directory to DIR";
    term.echo(output);
    return;
  } else {
    if (commands[0] == "~") {
      currentFolder = "~";
      term.push(what_to_do, {prompt: '[[b;#5fff00;#000]user]:[[b;#af00ff;#000]' + currentFolder + ']$ '});
      return;
    }

    if (commands[0].indexOf("..") > -1) {
      if (commands[0] == ".." || commands[0] == "../") {
        if (currentFolder != "/" || currentFolder != "~") {

          var temp = currentFolder.split("/");
          temp = temp.slice(0, temp.length-1);
          currentFolder = temp.join("/");
          console.log(currentFolder);
          term.push(what_to_do, {prompt: '[[b;#5fff00;#000]user]:[[b;#af00ff;#000]' + currentFolder + ']$ '});
        }
      } else {
        term.error("volevo andare indietro di più di una cartella e magari anche entrare in un altra. DA GESTIRE");
      }

      return;
    }


    // serve a rimuovere eventuale "/" in fondo
    if (commands[0][commands[0].length-1] == "/") {
      commands[0] = commands[0].slice(0, -1);
    }

    //
    if (checkExistingPath(commands[0])) {
      currentFolder = currentFolder + "/" + commands[0].replace(/#/g, "\\ ");
      term.push(what_to_do, {prompt: '[[b;#5fff00;#000]user]:[[b;#af00ff;#000]' + currentFolder + ']$ '});
    } else {
      if (commands[0][0] == "~") {
        var currentFolder_sv = currentFolder;
        currentFolder = "";
        if (checkExistingPath(commands[0].replace("~/", ""))){
          currentFolder = commands[0].replace(/#/g, "\\ ");
          term.push(what_to_do, {prompt: '[[b;#5fff00;#000]user]:[[b;#af00ff;#000]' + currentFolder + ']$ '});
        } else {
          currentFolder = currentFolder_sv;
          term.error("cd: " + commands[0] + ": Folder doesnt exist");
        }
      } else {
        term.error("cd: " + commands[0] + ": Folder doesnt exist");
      }
    }
  }
}

function help(term){
  var output = "List of commands\n";
  output += "Digit \"[name] --help\" to learn more about the function [name]\n";
  output += "\n";
  output += "clear".padding(5) + "\tClear the terminal screen\n";
  output += "ls".padding(5) + "\tList information about the current directory\n";
  output += "cd".padding(5) + "\tChange Directory\n";
  output += "pwd".padding(5) + "\tPrint name of current/working directory\n";
  output += "hash".padding(5) + "\tShow hash of the current File System\n";
  output += "info".padding(5) + "\tShow Information about the current Phone\n";
  output += "list".padding(5) + "\tShow the list of Devices\n";
  output += "get".padding(5) + "\tget will simply download the File specified on the command line\n";
  term.echo(output);
}

function info(term){
  var output = "Information about the current build, extracted from system properties\n";
  output += "\n";
  output += "Model: " + global_json["MODEL"].padding(20) + "\t";
  output += "Device: " + global_json["DEVICE"].padding(20) + "\t"; /*Codename*/
  output += "Manufacturer: " + global_json["MANUFACTURER"] + "\n";
  output += "Build Number: " + global_json["BUILDN"].padding(13) + "\t"; /*Build Number*/
  output += "Serial: " + global_json["SERIAL"].padding(20) + "\t";
  output += "Brand: " + global_json["BRAND"] + "\n";
  term.echo(output);
}

function list(term){
  $.ajax({
    async: false,
    type: 'GET',
    url: "http://127.0.0.1:8001/terminal?cmd=list",
    success: function(data, status){
      console.log("callback list function");
      data_t = data.split(":");
      list_t = data_t;
      out = '[[b;#ffffff;#000]List of Devices]\n';
      for (var i = 0; i < data_t.length-1; i++) {
        out += "-> " + data_t[i] + "\n";
      }
      out +=  data_t[data_t.length-1];
      out += "\nUse \"list [SERIAL]\" to see the associated File System Map";
      term.echo(out);
    }
  });
}

var possibilityOfWho = "";
var possibility = [];

function takeFileSystemJson(who, term){
  possibilityOfWho = who;
  $.ajax({
    async: false,
    type: 'GET',
    url: "http://127.0.0.1:8001/terminal?cmd=getjson&id=" + who,
    success: function(data, status){
      console.log("takeFileSystemJson callback " + data);
      if (data != "err") {
        var out = "";
        try {
          lista_t = JSON.parse(data);
        } catch (e) {
          term.echo("There are been some problem. Try again or type help");
          return;
        }
        lista_t = lista_t["lista"];
        for (var i = 0; i < lista_t.length; i++) {
          var temp = lista_t[i].split(":");
          out += i + "-> " + temp[0] + "\t" + formatTime(temp[1]) + "\n";
          possibility.push(temp[0]);
        }
        term.echo(out);
        term.echo("Use the command \"choose [NUM]\" to choose one of the above FileSystem");
      } else {
        term.echo("There has been some problem");
      }
    }
  });
}

function getFile(commands, term){
  if (commands.length === 0) {
    term.echo("No file chosen to download.");
    return;
  }

  if (commands.length > 1) {
    term.echo("There are been some error. Type \"getFile --help\"");
    return;
  }

  if (commands[0] == "--help") {
    var out = "Type \"get [FILENAME]\" to download the [FILENAME] file.\n";
    out += "The command returns a link in which you can find the chosen file";
    term.echo(out);
    return;
  }

//al momento assumiamo zero errori e solo nome file
  var pathFile = commands[0];

  pathFile = currentFolder + "/" + pathFile;

  console.log("pathFile " + pathFile);

  return; // da togliere ovviamente dopo
  $.ajax({
    async: false,
    type: 'GET',
    url: "http://127.0.0.1:8001/terminal?cmd=getfile&id=x&path=" + pathFile,
    success: function(data, status){
      console.log("callback getFile function");
    }
  });

}

function manager(command, term){
  console.log(command);
  if (command === "") {
    return;
  }

  var commands = command.split(" ").clean("");
  if (commands[0] == "list") {
    if (commands[1] == "--help") {
      var out = "";
      out += "\"list\" is a utility for displaying information about Devices\n\t";
      out += "in the app and the File System connected to them.";
      term.echo(out);
      return;
    }
    if (commands.length == 1) {
      list(term);
    } else {
      takeFileSystemJson(commands[1], term);
    }
    return;
  }

  if (commands[0] == "choose" && commands.length > 1) {
    if(possibility[commands[1]] === undefined){
      term.echo("It has not been selected a valid file. Try choose [NUM].\n[NUM] is the associated index to the list that you find on the left");
      return;
    }

    $.ajax({
      async: false,
      type: 'GET',
      url: "http://127.0.0.1:8001/terminal?cmd=getfs&id=" + possibilityOfWho + "/" + possibility[commands[1]],
      success: function(data, status){
        console.log("takeFileSystemJson callback " + data);
        try {
          global_json = JSON.parse(data);
          currentFolder = "~";
          term.push(what_to_do, {prompt: '[[b;#5fff00;#000]user]:[[b;#af00ff;#000]' + currentFolder + ']$ '});
        } catch (e) {
          term.echo("There are been some problem. Try again or type help");
          return;
        }
        term.echo("Loading Complete");
        console.log(global_json["sha1"]);
      }
    });
    return;
  }

  /*
  I comandi sono divisi in due gruppi con un controllo in mezzo nel caso in cui
  global_json sia undefined poichè quelli sotto non hanno senso se non è già
  stato scaricato un FileSystem
  */

  if (commands[0] == "help") {help(term); return;}
  else if (commands[0] == "takeAll") {takeAll(); return;}

  if (global_json === undefined) {
    term.echo("Non hai caricato nessun FileSystem. Usa \"list\" o \"help\"");
    return;
  }

  if (commands[0] == "ls") ls(commands.slice(1, commands.length), term);
  else if (commands[0] == "cd") cd(commands.slice(1, commands.length), term);
  else if (commands[0] == "get") getFile(commands.slice(1, commands.length), term);
  else if (commands[0] == "pwd") term.echo(currentFolder);
  else if (commands[0] == "hash") term.echo("The current FileSystem has hash: " + global_json["sha1"]);
  else if (commands[0] == "info") info(term);
  else term.echo("Your command doesn't exists. Try to type: help");
}
