# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  # All Vagrant configuration is done here. The most common configuration
  # options are documented and commented below. For a complete reference,
  # please see the online documentation at vagrantup.com.

  # Every Vagrant virtual environment requires a box to build off of.
  config.vm.box = "ubuntu/wily64"

  if Vagrant.has_plugin?("vagrant-cachier")
    config.cache.scope = :box
  end

  # Disable automatic box update checking. If you disable this, then
  # boxes will only be checked for updates when the user runs
  # `vagrant box outdated`. This is not recommended.
  # config.vm.box_check_update = false

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine. In the example below,
  # accessing "localhost:8080" will access port 80 on the guest machine.

  # Create a private network, which allows host-only access to the machine
  # using a specific IP.
  # config.vm.network "private_network", ip: "192.168.33.10"

  # Create a public network, which generally matched to bridged network.
  # Bridged networks make the machine appear as another physical device on
  # your network.
  # config.vm.network "public_network"

  # If true, then any SSH connections made will enable agent forwarding.
  # Default value: false
  # config.ssh.forward_agent = true

  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"

  # Provider-specific configuration so you can fine-tune various
  # backing providers for Vagrant. These expose provider-specific options.
  # Example for VirtualBox:
  #
  # config.vm.provider "virtualbox" do |vb|
  #   # Don't boot with headless mode
  #   vb.gui = true
  #
  #   # Use VBoxManage to customize the VM. For example to change memory:
  #   vb.customize ["modifyvm", :id, "--memory", "1024"]
  # end
  #


  config.vm.provision "shell", inline: <<SCRIPT
    set -x
    set -e
    # prepare innosetup (needs wine and an extractor)
    dpkg --add-architecture i386
    apt-get -y update
    apt-get install -y xvfb

    echo ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true | sudo debconf-set-selections
    apt-get -y install wine unzip unrar unp
    if [ ! -d /vagrant/installer/inno ]; then
        wget http://www.jrsoftware.org/download.php/is.exe
        wget http://downloads.sourceforge.net/project/innounp/innounp/innounp%200.45/innounp045.rar

        unp innounp045.rar
        wine innounp.exe -dinno -c"{app}" -x is.exe
        if [ -d /vagrant/installer/inno ]; then
            rm -r /vagrant/installer/inno
        fi
        mv inno /vagrant/installer/
        rm -f innounp.exe
        rm -f is.exe
    fi

    # download jre
    cd /vagrant/installer
    if [ ! -d jre ]; then
        wget -O jre.tar.gz \
            --no-cookies \
            --header "Cookie: oraclelicense=accept-securebackup-cookie" \
            http://download.oracle.com/otn-pub/java/jdk/8u73-b02/jre-8u73-windows-x64.tar.gz

        tar -xzf jre.tar.gz
        mv jre1.8.0_73 jre
        rm jre.tar.gz
    fi

    if [ ! -d launch4j ]; then
        wget -O launch4j.tgz https://sourceforge.net/projects/launch4j/files/launch4j-3/3.8/launch4j-3.8-linux.tgz
        tar -xzf launch4j.tgz
        rm launch4j.tgz
    fi

    # now prepare the build
    cd ..
    apt-get install -y openjdk-8-jdk \
        openjfx \
        python-dev \
        virtualenv \
        python3 \
        python3-virtualenv \
        python3-pip \
        python3-dev \
        python3.5 \
        python3.5-dev \
        python3.5-venv

    if [ ! -d ~/.virtualenv ]; then
        mkdir ~/.virtualenv
        mkdir /home/vagrant/.virtualenv
        echo "always-copy=true" > ~/.virtualenv/virtualenv.ini
        echo "always-copy=true" > /home/vagrant/.virtualenv/virtualenv.ini
    fi
    update-ca-certificates -f

    chown -R vagrant:vagrant /vagrant/installer
    echo "now enter the VM with `vagrant ssh`, go to `/vagrant/installer` and build with `bash build-setup.sh`"

SCRIPT
end
