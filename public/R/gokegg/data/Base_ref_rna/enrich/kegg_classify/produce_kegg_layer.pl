#!usr/bin/perl

use strict;

die "usage:perl $0 <br08901.keg> \n" unless @ARGV == 1;

open A,$ARGV[0];
my %h;my %rec;
my @b;my ($inf1, $inf2);
while(<A>){
    chomp;next if(/^#/);
    my @a=split /\s+/;
    if(/^A/){
        @b=split /[<>]/;
        $rec{$b[2]}=$.;
    }
    if(/^B/){
        $inf1=join " ",@a[1..$#a];
    }
    if(/^C/){
        $inf2=join " ",@a[2..$#a];
    }
    $h{$b[2]}{$inf1}{$inf2}=1;
    print "$b[2]\t$inf1\t$inf2\n";
}
close A;

foreach my $k1(sort {$rec{$a} <=> $rec{$b}} keys %rec){
    foreach my $k2(sort keys %{$h{$k1}}){
        foreach my $k3(sort keys %{$h{$k1}{$k2}}){
            #print "$k1\t$k2\t$k3\n";
        }
    }
}
