# manchester-opencast.spec
# Package Manchester's Opencast 2.x sans configuration files that are installed by Ansible

%define     _product_name manchester-opencast
%define     _prefix /opt/opencast
%define     _data_prefix /var/opencast
%define     __jar_repack 0

Name:        %{_product_name}

# The following lines are edited by the rpm-build.py script
Version:    CHANGE_ME_VERSION
Release:    CHANGE_ME_RELEASE
Summary:    Manchester Opencast
License:    ECL 2.0    
URL:        http://itservices.manchester.ac.uk    
BuildRoot:  %{_tmppath}/%{_product_name}-%{version}-%{release}
Source:     %{_product_name}-%{version}-%{release}.tar.gz

# Baseline requirements
Requires:   java-1.8.0-openjdk

%description
Manchester Opencast video management and processing system

%prep
%setup -n %{_product_name}-%{version}-%{release}

%build

%pre
#first install
if [ "$1" = "1" ]; then
  getent group opencast >/dev/null || groupadd opencast
  getent passwd opencast >/dev/null || useradd -d /opt/opencast -m -g opencast opencast -r -s /sbin/nologin -c "Opencast media processing"
  install -d -m 755 $RPM_BUILD_ROOT/var/log/opencast

#things to do before an update
elif [ "$1" = "2" ]; then
  #stop opencast
  systemctl stop opencast >/dev/null 2>&1
  #clear caches
  rm -rf /var/cache/opencast/* /var/tmp/opencast/* >/dev/null 2>&1
fi

%install
# Work directories
mkdir -p $RPM_BUILD_ROOT/var/cache/opencast
mkdir -p $RPM_BUILD_ROOT/var/tmp/opencast
mkdir -p $RPM_BUILD_ROOT/var/log/opencast

# Data directories
mkdir -p $RPM_BUILD_ROOT%{_data_prefix}/distribution/downloads
mkdir -p $RPM_BUILD_ROOT%{_data_prefix}/distribution/streams
mkdir -p $RPM_BUILD_ROOT%{_data_prefix}/archive
mkdir -p $RPM_BUILD_ROOT%{_data_prefix}/work/inbox
mkdir -p $RPM_BUILD_ROOT%{_data_prefix}/work/shared/workspace
mkdir -p $RPM_BUILD_ROOT%{_data_prefix}/work/shared/files

# Install Opencast
install -d -m 755 $RPM_BUILD_ROOT%{_prefix}
install -d -m 755 $RPM_BUILD_ROOT%{_unitdir}
install -d -m 755 $RPM_BUILD_ROOT/etc
install -d -m 755 $RPM_BUILD_ROOT%{_prefix}/docs/ddl
install -d -m 755 $RPM_BUILD_ROOT%{_prefix}/docs/upgrade

# Install the Opencast libraries
cp -rf lib $RPM_BUILD_ROOT%{_prefix}
cp -rf system $RPM_BUILD_ROOT%{_prefix}

# Opencast systemd scripts
install -D -m 755 bin/start-opencast $RPM_BUILD_ROOT%{_prefix}/bin/start-opencast
install -D -m 755 bin/stop-opencast $RPM_BUILD_ROOT%{_prefix}/bin/stop-opencast
install -D -m 644 docs/service/opencast.service $RPM_BUILD_ROOT%{_unitdir}/opencast.service

# Opencast configuration files
cp -rf etc $RPM_BUILD_ROOT%{_prefix}

# Database/update scripts
cp -rf docs/ddl/* $RPM_BUILD_ROOT%{_prefix}/docs/ddl/
cp -rf docs/upgrade/* $RPM_BUILD_ROOT%{_prefix}/docs/upgrade/

%post
# First install only
if [ "$1" =  "1" ]; then
  ln -s %{_prefix}/etc ${RPM_BUILD_ROOT}/etc/opencast
  systemctl enable opencast
elif [ "$1" =  "2" ]; then
  # restart opencast on updates
  systemctl restart opencast >/dev/null 2>&1
fi

%clean
rm -rf %{buildroot}

%files
%defattr(-,opencast,opencast,-)

%attr(0755,opencast,opencast) %{_prefix}
%attr(0755,opencast,opencast) /var/cache/opencast
%attr(0755,opencast,opencast) /var/tmp/opencast
%attr(0755,opencast,opencast) /var/log/opencast
# Commenting out adjustment of ownership on /var/opencast because many of the
# NFS mounted filesystems don't allow for adjustment of permissions and ownership
#%attr(0755,opencast,opencast) /var/opencast
%attr(0644,root,root) %{_unitdir}/opencast.service

%config(noreplace) %{_prefix}/etc/
%config(noreplace) %{_unitdir}/opencast.service

%preun
if [ "$1" = "0" ]; then
  systemctl stop opencast
fi

%postun
if [ "$1" = "0" ]; then
  systemctl disable opencast
  rm -rf /var/cache/opencast
  rm -rf /var/tmp/opencast
  rm -f  /etc/opencast
fi

%changelog
* Wed Jan 18 2017 James Perrin <james.perrin@manchester.ac.uk> - 2.0
- Version 2 for Opencast 2.x
* Thu Sep 20 2012 Jaime Gago <jaime@entwinemedia.com> - 1.0
- Version 1

